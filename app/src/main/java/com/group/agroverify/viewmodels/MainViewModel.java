package com.group.agroverify.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group.agroverify.models.Keystore;
import com.group.agroverify.service.KeyService;
import com.group.agroverify.utils.ApiClient;
import com.group.agroverify.utils.KeyStorage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {


    private final MutableLiveData<Keystore> keyStore = new MutableLiveData<>();


    public LiveData<Keystore> getKeyStore() { return keyStore; }
    public void fetchPublicKey() {
        KeyService service = ApiClient.getRetrofit().create(KeyService.class);
        Call<Keystore> call = service.getActivePublicKey();

        call.enqueue(new Callback<Keystore>() {
            @Override
            public void onResponse(Call<Keystore> call, Response<Keystore> response) {
                System.out.println(response.code());
                try {
                if (response.isSuccessful() && response.body() != null) {
                    keyStore.setValue(response.body());
                } else {
                    keyStore.setValue(null);
                }
                } catch (Exception e) {
                    keyStore.setValue(null);
                    Log.e("MAIN_VIEW_MODEL", "Failed to get Keystore: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Keystore> call, Throwable throwable) {
                keyStore.setValue(null);
            }
        });
    }

}
