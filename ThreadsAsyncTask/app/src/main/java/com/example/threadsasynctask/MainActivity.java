package com.example.threadsasynctask;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Vues liées à l'interface
    private TextView tvEtat;
    private ProgressBar barProgression;
    private ImageView ivResultat;

    // Handler pour poster sur le UI thread
    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Liaison vues XML ↔ Java
        tvEtat        = findViewById(R.id.tvEtat);
        barProgression = findViewById(R.id.barProgression);
        ivResultat    = findViewById(R.id.ivResultat);

        Button btnDemarrerThread = findViewById(R.id.btnDemarrerThread);
        Button btnLancerCalcul  = findViewById(R.id.btnLancerCalcul);
        Button btnToastRapide   = findViewById(R.id.btnToastRapide);

        // Handler lié au Main Thread (UI)
        uiHandler = new Handler(Looper.getMainLooper());

        // Toast immédiat — prouve que l'UI n'est pas bloquée
        btnToastRapide.setOnClickListener(v ->
                Toast.makeText(this, R.string.ui_reactive, Toast.LENGTH_SHORT).show()
        );

        // Lancer le chargement image dans un Thread de fond
        btnDemarrerThread.setOnClickListener(v -> lancerChargementImage());

        // Lancer le calcul lourd via AsyncTask
        btnLancerCalcul.setOnClickListener(v -> new TacheCalculLourd(this).execute());
    }

    // ─────────────────────────────────────────
    //  PARTIE 1 : THREAD MANUEL
    // ─────────────────────────────────────────
    private void lancerChargementImage() {
        barProgression.setVisibility(View.VISIBLE);
        barProgression.setProgress(0);
        tvEtat.setText(R.string.chargement_image_en_cours);

        new Thread(() -> {
            try { Thread.sleep(1500); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // MODIFICATION : Utiliser img_test (forme personnalisée) au lieu de l'icône par défaut
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.img_test);
            Bitmap bmp = null;

            if (drawable instanceof BitmapDrawable) {
                bmp = ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable != null) {
                // Conversion du Vector en Bitmap
                bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }

            final Bitmap finalBmp = bmp;
            uiHandler.post(() -> {
                if (finalBmp != null) {
                    ivResultat.setImageBitmap(finalBmp);
                }
                barProgression.setProgress(100);
                barProgression.setVisibility(View.INVISIBLE);
                tvEtat.setText(R.string.image_chargee);
            });
        }).start();
    }

    // ─────────────────────────────────────────
    //  PARTIE 2 : ASYNCTASK
    // ─────────────────────────────────────────
    @SuppressWarnings("deprecation")
    private static class TacheCalculLourd extends AsyncTask<Void, Integer, Long> {

        private final WeakReference<MainActivity> activityReference;

        TacheCalculLourd(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        // Exécuté sur le UI thread avant le calcul
        @Override
        protected void onPreExecute() {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.barProgression.setVisibility(View.VISIBLE);
            activity.barProgression.setProgress(0);
            activity.tvEtat.setText(R.string.calcul_demarre);
        }

        // Exécuté sur le Worker thread — jamais toucher l'UI ici
        @Override
        protected Long doInBackground(Void... params) {
            long cumul = 0;

            for (int iteration = 1; iteration <= 100; iteration++) {
                if (isCancelled()) break;

                // Calcul artificiel lourd
                for (int j = 0; j < 200_000; j++) {
                    cumul += (iteration * (long)j) % 13;
                }

                // Envoie la progression au UI thread
                publishProgress(iteration);
            }

            return cumul;
        }

        // Mis à jour de la barre — UI thread
        @Override
        protected void onProgressUpdate(Integer... avancement) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.barProgression.setProgress(avancement[0]);
            activity.tvEtat.setText(activity.getString(R.string.calcul_en_cours, avancement[0]));
        }

        // Fin du calcul — UI thread
        @Override
        protected void onPostExecute(Long resultat) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.barProgression.setVisibility(View.INVISIBLE);
            activity.tvEtat.setText(activity.getString(R.string.calcul_termine, resultat));
        }
    }
}
