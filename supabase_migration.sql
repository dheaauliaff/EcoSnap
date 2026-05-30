-- ══════════════════════════════════════════════════════════════════
-- EcoSnap — Supabase SQL Migration
-- Jalankan di: Supabase Dashboard → SQL Editor
-- ══════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────
-- 1. TAMBAH KOLOM is_approved DI TABEL USER
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE "user"
ADD COLUMN IF NOT EXISTS is_approved BOOLEAN NOT NULL DEFAULT FALSE;

-- Approve semua user existing (opsional — hapus baris ini jika ingin semua user baru butuh approval)
-- UPDATE "user" SET is_approved = TRUE;

-- ─────────────────────────────────────────────────────────────────
-- 2. FUNGSI: APPROVE USER (dipanggil Admin via Supabase Dashboard atau API)
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION approve_user(target_firebase_uid TEXT)
RETURNS VOID AS $$
BEGIN
  UPDATE "user"
  SET is_approved = TRUE
  WHERE firebase_uid = target_firebase_uid;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Contoh penggunaan:
-- SELECT approve_user('firebase-uid-user-disini');

-- ─────────────────────────────────────────────────────────────────
-- 3. RLS POLICY: ROLE EKSEKUTIF (Read-Only)
-- ─────────────────────────────────────────────────────────────────

-- Aktifkan RLS
ALTER TABLE scan_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE "user"       ENABLE ROW LEVEL SECURITY;

-- Policy: Semua authenticated user bisa SELECT scan_history
-- (filter lebih ketat bisa ditambahkan sesuai kebutuhan)
CREATE POLICY IF NOT EXISTS "authenticated_read_scan"
ON scan_history FOR SELECT
TO authenticated
USING (true);

-- Policy: Hanya non-eksekutif yang bisa INSERT
CREATE POLICY IF NOT EXISTS "blokir_insert_eksekutif"
ON scan_history FOR INSERT
TO authenticated
WITH CHECK (
  (auth.jwt() ->> 'role') != 'eksekutif'
);

-- Policy: Hanya non-eksekutif yang bisa UPDATE
CREATE POLICY IF NOT EXISTS "blokir_update_eksekutif"
ON scan_history FOR UPDATE
TO authenticated
USING (
  (auth.jwt() ->> 'role') != 'eksekutif'
);

-- Policy: Hanya non-eksekutif yang bisa DELETE
CREATE POLICY IF NOT EXISTS "blokir_delete_eksekutif"
ON scan_history FOR DELETE
TO authenticated
USING (
  (auth.jwt() ->> 'role') != 'eksekutif'
);

-- ─────────────────────────────────────────────────────────────────
-- 4. FUNGSI: RIWAYAT USER TANPA DUPLIKAT
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION get_riwayat_user(uid TEXT)
RETURNS TABLE (
  id          TEXT,
  firebase_id TEXT,
  rw_id       TEXT,
  rt_id       TEXT,
  wilayah     TEXT,
  nama_sampah TEXT,
  kategori    TEXT,
  image_url   TEXT,
  confidence  FLOAT,
  latitude    DOUBLE PRECISION,
  longitude   DOUBLE PRECISION,
  created_at  TIMESTAMPTZ
) AS $$
  SELECT DISTINCT ON (id)
    id, firebase_id, rw_id, rt_id, wilayah,
    nama_sampah, kategori, image_url, confidence,
    latitude, longitude, created_at
  FROM scan_history
  WHERE firebase_id = uid
  ORDER BY id, created_at DESC;
$$ LANGUAGE sql SECURITY DEFINER;

-- ─────────────────────────────────────────────────────────────────
-- 5. FUNGSI: AGREGAT PER RT (untuk Maps — hierarki wilayah)
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION get_agregat_per_rt()
RETURNS TABLE(
  rt_id      TEXT,
  rw_id      TEXT,
  total_scan BIGINT,
  dominan    TEXT,
  avg_lat    DOUBLE PRECISION,
  avg_lng    DOUBLE PRECISION
) AS $$
  SELECT
    rt_id,
    rw_id,
    COUNT(*) AS total_scan,
    MODE() WITHIN GROUP (ORDER BY nama_sampah) AS dominan,
    AVG(latitude)  AS avg_lat,
    AVG(longitude) AS avg_lng
  FROM scan_history
  WHERE rt_id IS NOT NULL
    AND latitude  IS NOT NULL
    AND longitude IS NOT NULL
  GROUP BY rt_id, rw_id
  ORDER BY total_scan DESC;
$$ LANGUAGE sql SECURITY DEFINER;

-- ─────────────────────────────────────────────────────────────────
-- 6. FUNGSI: DATA PER LOKASI (orientasi koordinat → data)
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION get_data_per_lokasi()
RETURNS TABLE(
  rt_id         TEXT,
  rw_id         TEXT,
  wilayah       TEXT,
  nama_sampah   TEXT,
  jumlah        BIGINT,
  avg_lat       DOUBLE PRECISION,
  avg_lng       DOUBLE PRECISION,
  terakhir_scan TIMESTAMPTZ
) AS $$
  SELECT
    rt_id,
    rw_id,
    wilayah,
    nama_sampah,
    COUNT(*) AS jumlah,
    AVG(latitude)  AS avg_lat,
    AVG(longitude) AS avg_lng,
    MAX(created_at) AS terakhir_scan
  FROM scan_history
  WHERE rt_id IS NOT NULL AND wilayah IS NOT NULL
  GROUP BY rt_id, rw_id, wilayah, nama_sampah
  ORDER BY rt_id, jumlah DESC;
$$ LANGUAGE sql SECURITY DEFINER;

-- ══════════════════════════════════════════════════════════════════
-- SELESAI — Semua migration siap dijalankan
-- ══════════════════════════════════════════════════════════════════
