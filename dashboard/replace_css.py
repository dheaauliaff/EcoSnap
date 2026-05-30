import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

new_css = """  <style>
    :root {
      --green-900: #1B5E20;
      --green-700: #2E7D32;
      --green-600: #388E3C;
      --green-500: #4CAF50;
      --green-200: #A5D6A7;
      --green-100: #C8E6C9;
      --green-50:  #F1F8F2;
      --surface:   #FFFFFF;
      --bg:        #F8FAF8;
      --border:    #E4EDE5;
      --border-2:  #CDE0CE;
      --text-1: #111814;
      --text-2: #3D5C40;
      --text-3: #7A9B7C;
      --shadow-xs: 0 1px 2px rgba(0,0,0,.04);
      --shadow-sm: 0 1px 4px rgba(0,0,0,.07);
      --shadow-md: 0 4px 14px rgba(0,0,0,.08);
      --r-sm: 8px; --r-md: 12px; --r-lg: 18px;
      --c-organik:#16a34a; --c-plastik:#ea580c; --c-kertas:#ca8a04;
      --c-kaca:#0891b2; --c-kardus:#2563eb; --c-logam:#7c3aed;
      --sidebar-w: 240px; --sidebar-collapsed: 60px;
    }
    *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
    html{scroll-behavior:smooth}
    body{font-family:'Inter',sans-serif;background:var(--bg);color:var(--text-1);min-height:100vh;overflow-x:hidden;-webkit-font-smoothing:antialiased}

    /* SIDEBAR */
    .sidebar{position:fixed;top:0;left:0;width:var(--sidebar-w);height:100vh;background:var(--green-900);display:flex;flex-direction:column;z-index:100;overflow:hidden;transition:width .22s cubic-bezier(.4,0,.2,1)}
    .sidebar.collapsed{width:var(--sidebar-collapsed)}
    .sidebar.collapsed .logo-text,.sidebar.collapsed .nav-label-text,.sidebar.collapsed .nav-section-label,.sidebar.collapsed .sync-text,.sidebar.collapsed .last-sync{display:none}
    .sidebar.collapsed .nav-item{justify-content:center;padding:10px 0}
    .sidebar.collapsed .sidebar-logo{justify-content:center;padding:18px 0 14px}
    .sidebar.collapsed .sidebar-footer{padding:12px 0;display:flex;flex-direction:column;align-items:center}
    .sidebar-logo{padding:18px 20px 14px;display:flex;align-items:center;gap:10px;border-bottom:1px solid rgba(255,255,255,.08);position:relative}
    .sidebar-toggle{position:absolute;right:-10px;top:50%;transform:translateY(-50%);width:20px;height:20px;border-radius:50%;background:var(--green-500);border:2px solid #fff;color:#fff;display:flex;align-items:center;justify-content:center;cursor:pointer;font-size:9px;box-shadow:var(--shadow-sm);z-index:10;transition:all .22s;line-height:1}
    .sidebar.collapsed .sidebar-toggle{transform:translateY(-50%) scaleX(-1)}
    .logo-icon{width:34px;height:34px;background:rgba(255,255,255,.12);border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:18px;border:1px solid rgba(255,255,255,.15);flex-shrink:0}
    .logo-text{overflow:hidden}
    .logo-name{font-family:'Plus Jakarta Sans',sans-serif;font-weight:700;font-size:15px;color:#fff;white-space:nowrap;letter-spacing:-.2px}
    .logo-sub{font-size:9px;color:rgba(255,255,255,.45);text-transform:uppercase;letter-spacing:1px;font-weight:500;white-space:nowrap}
    .sidebar-nav{flex:1;padding:10px 8px;display:flex;flex-direction:column;gap:1px;overflow-y:auto;overflow-x:hidden}
    .nav-section-label{font-size:9px;text-transform:uppercase;letter-spacing:1.5px;color:rgba(255,255,255,.28);font-weight:600;padding:12px 10px 4px;white-space:nowrap}
    .nav-item{display:flex;align-items:center;gap:10px;padding:8px 10px;border-radius:var(--r-sm);cursor:pointer;transition:background .15s,color .15s;color:rgba(255,255,255,.60);font-size:13px;font-weight:500;border:none;background:none;width:100%;text-align:left;text-decoration:none;white-space:nowrap;overflow:hidden}
    .nav-item:hover{background:rgba(255,255,255,.08);color:rgba(255,255,255,.90)}
    .nav-item.active{background:rgba(255,255,255,.14);color:#fff;font-weight:600}
    .nav-item.active .nav-icon{background:rgba(255,255,255,.18)}
    .nav-icon{width:28px;height:28px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:14px;background:rgba(255,255,255,.06);flex-shrink:0}
    .sidebar-footer{padding:12px 16px 16px;border-top:1px solid rgba(255,255,255,.08)}
    .sync-status{display:flex;align-items:center;gap:7px;font-size:11px;color:rgba(255,255,255,.45)}
    .sync-text{white-space:nowrap;overflow:hidden;font-size:11px}
    .sync-dot{width:6px;height:6px;border-radius:50%;background:var(--green-500);box-shadow:0 0 0 2px rgba(76,175,80,.25);animation:pulse-dot 2s infinite;flex-shrink:0}
    .sync-dot.offline{background:#ef4444;box-shadow:0 0 0 2px rgba(239,68,68,.25)}
    @keyframes pulse-dot{0%,100%{opacity:1}50%{opacity:.4}}
    .last-sync{font-size:10px;color:rgba(255,255,255,.25);margin-top:2px;white-space:nowrap}

    /* MAIN */
    .main{margin-left:var(--sidebar-w);min-height:100vh;display:flex;flex-direction:column;width:calc(100vw - var(--sidebar-w));max-width:calc(100vw - var(--sidebar-w));overflow-x:hidden;transition:margin-left .22s cubic-bezier(.4,0,.2,1),width .22s cubic-bezier(.4,0,.2,1),max-width .22s cubic-bezier(.4,0,.2,1)}
    .sidebar.collapsed ~ .main{margin-left:var(--sidebar-collapsed);width:calc(100vw - var(--sidebar-collapsed));max-width:calc(100vw - var(--sidebar-collapsed))}

    /* TOPBAR */
    .topbar{position:sticky;top:0;background:rgba(255,255,255,.97);backdrop-filter:blur(16px);border-bottom:1px solid var(--border);padding:0 28px;height:52px;display:flex;align-items:center;justify-content:space-between;z-index:50;gap:12px}
    .topbar-left{min-width:0;flex:1;overflow:hidden}
    .topbar-left h1{font-family:'Plus Jakarta Sans',sans-serif;font-size:15px;font-weight:700;color:var(--text-1);letter-spacing:-.2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
    .topbar-left .topbar-sub{font-size:11px;color:var(--text-3);white-space:nowrap}
    .topbar-right{display:flex;align-items:center;gap:6px;flex-shrink:0}
    .btn-refresh{display:flex;align-items:center;gap:5px;background:var(--green-900);color:#fff;border:none;border-radius:var(--r-sm);padding:6px 12px;font-size:12px;font-weight:600;cursor:pointer;transition:background .15s,transform .1s;font-family:'Inter',sans-serif;white-space:nowrap}
    .btn-refresh:hover{background:var(--green-700)}
    .btn-refresh:active{transform:scale(.97)}
    .btn-refresh svg{flex-shrink:0}
    .btn-refresh.loading svg{animation:spin .7s linear infinite}
    @keyframes spin{to{transform:rotate(360deg)}}
    .filter-chip{display:flex;align-items:center;gap:4px;background:var(--surface);border:1px solid var(--border);border-radius:var(--r-sm);padding:5px 10px;font-size:12px;font-weight:500;color:var(--text-2);cursor:pointer;transition:border-color .15s;flex-shrink:0}
    .filter-chip:hover{border-color:var(--green-500)}
    .filter-chip select{border:none;outline:none;background:transparent;font-size:12px;font-weight:500;color:var(--text-2);cursor:pointer;font-family:'Inter',sans-serif;max-width:90px}

    /* CONTENT */
    .content{padding:24px 28px;flex:1;min-width:0;overflow-x:hidden}
    .section{display:none}
    .section.active{display:block}

    /* KPI */
    .kpi-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:20px}
    .kpi-card{background:var(--surface);border-radius:var(--r-md);padding:20px 22px;border:1px solid var(--border);position:relative;overflow:hidden;transition:border-color .15s,box-shadow .15s}
    .kpi-card:hover{border-color:var(--border-2);box-shadow:var(--shadow-sm)}
    .kpi-card::after{content:'';position:absolute;left:0;top:0;bottom:0;width:3px;background:var(--kpi-color,var(--green-500));border-radius:0 2px 2px 0}
    .kpi-card.green{--kpi-color:var(--green-500)}
    .kpi-card.orange{--kpi-color:#f97316}
    .kpi-card.blue{--kpi-color:#3b82f6}
    .kpi-card.purple{--kpi-color:#8b5cf6}
    .kpi-label{font-size:11px;text-transform:uppercase;letter-spacing:.8px;color:var(--text-3);font-weight:600;margin-bottom:8px}
    .kpi-value{font-family:'Plus Jakarta Sans',sans-serif;font-size:30px;font-weight:800;color:var(--text-1);letter-spacing:-1.5px;line-height:1;margin-bottom:8px}
    .kpi-change{font-size:12px;color:var(--text-3);display:flex;align-items:center;gap:4px}
    .kpi-icon{position:absolute;right:16px;top:16px;font-size:28px;opacity:.08}
    .kpi-trend{display:inline-flex;align-items:center;gap:3px;font-size:11px;font-weight:600;padding:2px 7px;border-radius:20px}
    .kpi-trend.up{background:#dcfce7;color:#15803d}
    .kpi-trend.neutral{background:#f3f4f6;color:#6b7280}

    /* CARDS */
    .chart-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin-bottom:16px}
    .chart-grid.three-col{grid-template-columns:minmax(0,60%) minmax(0,40%)}
    .card{background:var(--surface);border-radius:var(--r-md);border:1px solid var(--border);overflow:hidden;transition:border-color .15s}
    .card:hover{border-color:var(--border-2)}
    .card-header{padding:16px 20px 0;display:flex;align-items:flex-start;justify-content:space-between;gap:8px}
    .card-title{font-family:'Plus Jakarta Sans',sans-serif;font-size:13px;font-weight:700;color:var(--text-1);letter-spacing:-.1px}
    .card-subtitle{font-size:11px;color:var(--text-3);margin-top:2px}
    .card-badge{background:var(--green-50);color:var(--green-700);font-size:10px;font-weight:600;padding:3px 8px;border-radius:20px;white-space:nowrap;flex-shrink:0;border:1px solid var(--green-100)}
    .card-body{padding:14px 20px 18px}
    .card-body.no-padding{padding:0}

    /* DONUT */
    .donut-wrap{display:flex;align-items:center;gap:16px;flex-wrap:wrap}
    .donut-chart-box{width:150px;height:150px;flex-shrink:0;position:relative}
    .donut-center-label{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);text-align:center;pointer-events:none}
    .donut-center-value{font-family:'Plus Jakarta Sans',sans-serif;font-size:22px;font-weight:800;color:var(--text-1);line-height:1}
    .donut-center-text{font-size:10px;color:var(--text-3);margin-top:3px}
    .legend-list{flex:1;display:flex;flex-direction:column;gap:8px}
    .legend-item{display:flex;align-items:center;gap:8px}
    .legend-dot{width:8px;height:8px;border-radius:50%;flex-shrink:0}
    .legend-name{flex:1;font-size:12px;color:var(--text-2);font-weight:500}
    .legend-value{font-size:12px;font-weight:700;color:var(--text-1)}
    .legend-pct{font-size:11px;color:var(--text-3);width:32px;text-align:right}

    /* CHARTS + MAP */
    .chart-canvas-wrap{position:relative;height:210px}
    #map{height:460px;width:100%;border-radius:0 0 var(--r-md) var(--r-md)}
    .map-controls{display:flex;gap:6px;flex-wrap:wrap}
    .map-btn{padding:5px 12px;border-radius:20px;font-size:11px;font-weight:600;border:1px solid var(--border);background:var(--surface);color:var(--text-2);cursor:pointer;transition:all .15s}
    .map-btn.active,.map-btn:hover{background:var(--green-900);color:#fff;border-color:var(--green-900)}

    /* TABLES */
    .rank-table,.scan-table{width:100%;border-collapse:collapse}
    .rank-table th,.scan-table th{text-align:left;font-size:10px;text-transform:uppercase;letter-spacing:.8px;color:var(--text-3);font-weight:600;padding:10px 16px;border-bottom:1px solid var(--border);background:var(--bg);white-space:nowrap}
    .rank-table td,.scan-table td{padding:11px 16px;font-size:13px;border-bottom:1px solid var(--border);vertical-align:middle}
    .rank-table tr:last-child td,.scan-table tr:last-child td{border-bottom:none}
    .rank-table tr:hover td,.scan-table tr:hover td{background:var(--green-50)}
    .scan-table th{padding:10px 20px;position:sticky;top:0;z-index:2}
    .scan-table td{padding:11px 20px}
    .rank-medal{font-size:18px}
    .rank-name{font-weight:600;color:var(--text-1);font-size:13px}
    .rank-sub{font-size:11px;color:var(--text-3);margin-top:1px}
    .rank-count{font-weight:700;color:var(--green-700);font-size:15px}
    .rank-bar-wrap{display:flex;align-items:center;gap:8px}
    .rank-bar{flex:1;height:4px;background:var(--green-100);border-radius:2px;overflow:hidden;min-width:60px}
    .rank-bar-fill{height:100%;background:var(--green-500);border-radius:2px;transition:width .5s ease}
    .kategori-pill{display:inline-flex;align-items:center;gap:4px;padding:2px 8px;border-radius:20px;font-size:11px;font-weight:600}
    .thumb{width:40px;height:40px;border-radius:8px;object-fit:cover;background:var(--green-100);border:1px solid var(--border)}
    .thumb-placeholder{width:40px;height:40px;border-radius:8px;background:var(--green-50);border:1px solid var(--border);display:flex;align-items:center;justify-content:center;font-size:16px}
    .confidence-bar-wrap{display:flex;align-items:center;gap:6px}
    .confidence-bar{width:56px;height:4px;background:var(--border);border-radius:2px;overflow:hidden}
    .confidence-fill{height:100%;border-radius:2px;background:var(--green-500)}

    /* SKELETON + EMPTY */
    .skeleton{background:linear-gradient(90deg,#f0f4f0 25%,#e4ede4 50%,#f0f4f0 75%);background-size:200% 100%;animation:shimmer 1.4s infinite;border-radius:6px}
    @keyframes shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}
    .skeleton-text{height:12px;margin:5px 0}
    .skeleton-val{height:30px;width:70px}
    .empty-state{text-align:center;padding:40px 20px;color:var(--text-3)}
    .empty-icon{font-size:40px;margin-bottom:10px}
    .empty-title{font-size:14px;font-weight:600;color:var(--text-2)}
    .empty-sub{font-size:12px;margin-top:4px}

    /* MAP POPUP */
    .leaflet-popup-content-wrapper{border-radius:var(--r-sm) !important;box-shadow:var(--shadow-md) !important;border:1px solid var(--border) !important}
    .leaflet-popup-content{margin:14px 18px !important}
    .popup-title{font-family:'Plus Jakarta Sans',sans-serif;font-size:14px;font-weight:700;color:var(--text-1);margin-bottom:8px}
    .popup-row{display:flex;justify-content:space-between;font-size:12px;color:#555;margin-bottom:3px;gap:12px}
    .popup-badge{display:inline-block;padding:2px 8px;border-radius:20px;font-size:10px;font-weight:700;background:var(--green-50);color:var(--green-700);border:1px solid var(--green-100);margin-top:8px}

    /* SCROLLBAR */
    ::-webkit-scrollbar{width:5px;height:5px}
    ::-webkit-scrollbar-track{background:transparent}
    ::-webkit-scrollbar-thumb{background:var(--green-200);border-radius:3px}
    ::-webkit-scrollbar-thumb:hover{background:var(--green-500)}

    /* TIMELINE */
    .timeline{display:flex;flex-direction:column}
    .timeline-item{display:flex;gap:12px;padding:0 0 16px;position:relative}
    .timeline-item:not(:last-child)::after{content:'';position:absolute;left:15px;top:32px;bottom:0;width:1px;background:var(--border)}
    .timeline-icon{width:32px;height:32px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:15px;flex-shrink:0;background:var(--green-50);border:1px solid var(--border);z-index:1}
    .timeline-content{flex:1;padding-top:4px}
    .timeline-title{font-size:13px;font-weight:600;color:var(--text-1)}
    .timeline-time{font-size:11px;color:var(--text-3);margin-top:1px}
    .timeline-detail{font-size:11px;color:var(--text-2);margin-top:4px}

    /* CAT GRID */
    .cat-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}
    .cat-card{background:var(--bg);border:1px solid var(--border);border-radius:var(--r-sm);padding:14px 16px;transition:border-color .15s}
    .cat-card:hover{border-color:var(--cat-color)}
    .cat-icon{font-size:20px;margin-bottom:8px}
    .cat-name{font-size:12px;font-weight:600;color:var(--text-2);margin-bottom:4px}
    .cat-count{font-family:'Plus Jakarta Sans',sans-serif;font-size:24px;font-weight:800;color:var(--cat-color)}
    .cat-pct{font-size:11px;color:var(--text-3)}

    /* STAT ROWS */
    .stat-row{display:flex;align-items:center;gap:10px;margin-bottom:12px}
    .stat-row:last-child{margin-bottom:0}
    .stat-row-left{display:flex;align-items:center;gap:7px;width:100px;flex-shrink:0}
    .stat-dot{width:8px;height:8px;border-radius:50%;flex-shrink:0}
    .stat-name{font-size:12px;color:var(--text-2);font-weight:500}
    .stat-prog{flex:1;height:5px;background:var(--border);border-radius:3px;overflow:hidden}
    .stat-prog-fill{height:100%;border-radius:3px;transition:width .7s cubic-bezier(.4,0,.2,1)}
    .stat-cnt{width:38px;text-align:right;font-size:12px;font-weight:700;color:var(--text-1)}
    .stat-pct{width:32px;text-align:right;font-size:11px;color:var(--text-3)}

    /* RESPONSIVE */
    @media(max-width:1180px){.kpi-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
    @media(max-width:900px){.chart-grid,.chart-grid.three-col{grid-template-columns:minmax(0,1fr)}}
    @media(max-width:768px){
      .sidebar{width:var(--sidebar-collapsed) !important}
      .sidebar .logo-text,.sidebar .nav-section-label,.sidebar .sync-text,.sidebar .last-sync{display:none !important}
      .sidebar .nav-item{justify-content:center;padding:10px 0}
      .sidebar .sidebar-logo{justify-content:center;padding:18px 0 14px}
      .sidebar .sidebar-footer{padding:12px 0;display:flex;flex-direction:column;align-items:center}
      .main{margin-left:var(--sidebar-collapsed) !important;width:calc(100vw - var(--sidebar-collapsed)) !important;max-width:calc(100vw - var(--sidebar-collapsed)) !important}
      .kpi-grid{grid-template-columns:repeat(2,minmax(0,1fr))}
      .content{padding:14px 16px}
    }
    @media(max-width:480px){
      .kpi-grid{grid-template-columns:minmax(0,1fr)}
      .cat-grid{grid-template-columns:repeat(2,minmax(0,1fr))}
      .topbar{padding:0 14px}
    }
  </style>"""

# Replace <style>...</style> block
new_content = re.sub(r'<style>.*?</style>', new_css, content, flags=re.DOTALL)

with open('index.html', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Done! CSS replaced successfully.")
