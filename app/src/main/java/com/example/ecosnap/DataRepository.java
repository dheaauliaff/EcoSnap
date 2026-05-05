package com.example.ecosnap;

<<<<<<< HEAD
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.model.User;
=======
>>>>>>> 5267092143cead4c49f0890c2914264aa129435c
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import retrofit2.Call;

public class DataRepository {

    private static DataRepository instance;
    private final ApiService apiService;
    private final FirebaseAuth auth;

    private DataRepository() {
        apiService = RetrofitClient.getClient().create(ApiService.class);
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public Call<List<User>> getUser() {
        String uid = getCurrentUserId();
        if (uid == null) return null;
        return apiService.getUserByFirebaseUid("eq." + uid);
    }

    public Call<List<ScanHistory>> getStats() {
        String uid = getCurrentUserId();
        if (uid == null) return null;
        return apiService.getScanByUser("eq." + uid);
    }

    public Call<List<ScanHistory>> getLastScan() {
        String uid = getCurrentUserId();
        if (uid == null) return null;
        return apiService.getScanTerakhir("eq." + uid);
    }

    public Call<List<ScanHistory>> getMapData() {
        return apiService.getAllScans();
    }
}
