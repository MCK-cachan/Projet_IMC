package main.IMC;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.myapplication2.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

import main.appTool.toolbar;
import main.appTool.toolbar_music;

public class IMCMain extends AppCompatActivity {

    private Spinner             spinnerGenre;
    private TextInputEditText   editDateNaissance;
    private TextInputEditText   editPoids;
    private TextInputEditText   editTaille;
    private RadioGroup          radioGroupUnite;
    private RadioButton         radioCentimetre;
    private CheckBox            checkBoxAffichage;
    private TextView            textResultat;
    private Button              boutonCalculer;
    private Button              boutonRAZ;

    private double              imcCalcule = -1;
    private String              prenom     = "";

    // Clé pour la sauvegarde interne
    private static final String IMCdata = "IMCdata";
    private static final String PREF_NAME = "IMC_prefs";

    // TextWatcher générique pour remettre le texte par défaut
    private final TextWatcher resetWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            imcCalcule = -1;
            textResultat.setText(R.string.texte_defaut_resultat);
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // 1. Initialisation des outils et barres
        toolbar.init(this);
        toolbar_music.init(this);

        // Insets - Appliquer sur la racine pour éviter les décalages
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.second_main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, 0); 
            return insets;
        });

        // Récupérer le prénom passé en extra
        prenom = getIntent().getStringExtra("prenom") != null
                ? getIntent().getStringExtra("prenom") : "";

        // Liaisons vues
        spinnerGenre      = findViewById(R.id.spinnerGenre);
        editDateNaissance  = findViewById(R.id.editTextDateNaissance);
        editPoids         = findViewById(R.id.editTextPoids);
        editTaille        = findViewById(R.id.editTextTaille);
        radioGroupUnite   = findViewById(R.id.radioGroupUnite);
        radioCentimetre   = findViewById(R.id.radioCentimetre);
        checkBoxAffichage = findViewById(R.id.checkBoxAffichage);
        textResultat      = findViewById(R.id.textViewResultat);
        boutonCalculer    = findViewById(R.id.buttonCalculer);
        boutonRAZ         = findViewById(R.id.buttonRAZ);

        // Charger le dernier IMC sauvegardé
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        float savedIMC = prefs.getFloat(IMCdata, -1f);
        if (savedIMC > 0) {
            imcCalcule = savedIMC;
            afficherResultat(imcCalcule);
        }

        // Spinner Genre
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.genres_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(adapter);

        // Centimètre par défaut
        if (radioCentimetre != null) radioCentimetre.setChecked(true);

        // Date picker sur le champ date naissance
        if (editDateNaissance != null) {
            editDateNaissance.setOnClickListener(v -> ouvrirDatePicker());
            editDateNaissance.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) ouvrirDatePicker();
            });
            editDateNaissance.addTextChangedListener(resetWatcher);
        }

        // TextWatchers pour remettre résultat à défaut
        if (editPoids != null) editPoids.addTextChangedListener(resetWatcher);
        if (editTaille != null) editTaille.addTextChangedListener(resetWatcher);
        
        if (checkBoxAffichage != null) {
            checkBoxAffichage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (imcCalcule > 0) afficherResultat(imcCalcule);
            });
        }
        
        if (radioGroupUnite != null) {
            radioGroupUnite.setOnCheckedChangeListener((group, checkedId) -> {
                imcCalcule = -1;
                textResultat.setText(R.string.texte_defaut_resultat);
            });
        }

        // Bouton Calculer
        if (boutonCalculer != null) boutonCalculer.setOnClickListener(v -> calculerIMC());

        // Bouton RAZ
        if (boutonRAZ != null) boutonRAZ.setOnClickListener(v -> confirmerRAZ());
    }

    @Override
    public void onBackPressed() {
        retournerAvecResultat();
    }

    private void retournerAvecResultat() {
        Intent resultIntent = new Intent();
        if (imcCalcule > 0) {
            resultIntent.putExtra("imc_result", imcCalcule);
        }
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void ouvrirDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
            editDateNaissance.setText(date);
        }, cal.get(Calendar.YEAR) - 30, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ---- Calcul IMC ----
    private void calculerIMC() {
        String poidsStr  = editPoids.getText() != null ? editPoids.getText().toString().trim() : "";
        String tailleStr = editTaille.getText() != null ? editTaille.getText().toString().trim() : "";
        String dateStr   = editDateNaissance.getText() != null ? editDateNaissance.getText().toString().trim() : "";

        // Validation
        if (poidsStr.isEmpty() || tailleStr.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(this, R.string.erreur_champs_vides, Toast.LENGTH_SHORT).show();
            return;
        }

        double poids, taille;
        try {
            poids  = Double.parseDouble(poidsStr);
            taille = Double.parseDouble(tailleStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.erreur_valeurs_invalides, Toast.LENGTH_SHORT).show();
            return;
        }

        if (poids <= 0 || taille <= 0) {
            Toast.makeText(this, R.string.erreur_valeurs_zero, Toast.LENGTH_SHORT).show();
            return;
        }

        // Conversion taille si centimètre
        if (radioCentimetre.isChecked()) {
            taille = taille / 100.0;
        }

        imcCalcule = poids / (taille * taille);
        
        // Sauvegarde de l'IMC
        saveIMC(imcCalcule);
        
        afficherResultat(imcCalcule);
    }

    private void saveIMC(double imc) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putFloat(IMCdata, (float) imc)
                .apply();
    }

    private void afficherResultat(double imc) {
        if (!checkBoxAffichage.isChecked()) {
            textResultat.setText(getString(R.string.texte_resultat_simple, String.format("%.2f", imc)));
            return;
        }

        // Mode détaillé : genre + catégorie d'âge
        String genre = spinnerGenre.getSelectedItem().toString();
        String civilite = genre.equals("Homme") ? getString(R.string.monsieur) : getString(R.string.madame);

        int age = calculerAge();
        String categorie = getCategorie(imc, age);
        String trancheAge = getTranche(age);

        String texte = getString(R.string.texte_resultat_detail,
                civilite,
                String.format("%.2f", imc),
                trancheAge,
                categorie);
        textResultat.setText(texte);
    }

    private int calculerAge() {
        String dateStr = editDateNaissance.getText() != null
                ? editDateNaissance.getText().toString().trim() : "";
        try {
            String[] parts = dateStr.split("/");
            int jour  = Integer.parseInt(parts[0]);
            int mois  = Integer.parseInt(parts[1]) - 1;
            int annee = Integer.parseInt(parts[2]);

            Calendar naissance = Calendar.getInstance();
            naissance.set(annee, mois, jour);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - naissance.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < naissance.get(Calendar.DAY_OF_YEAR)) age--;
            return age;
        } catch (Exception e) {
            return 30; // valeur par défaut
        }
    }

    private String getCategorie(double imc, int age) {
        if (age < 35) {
            if (imc < 18.5) return getString(R.string.insuff_ponderale);
            if (imc < 25)   return getString(R.string.poids_normal);
            if (imc < 30)   return getString(R.string.surpoids);
            return getString(R.string.obesite);
        } else if (age < 65) {
            if (imc < 19)   return getString(R.string.insuff_ponderale);
            if (imc < 26)   return getString(R.string.poids_normal);
            if (imc < 31)   return getString(R.string.surpoids);
            return getString(R.string.obesite);
        } else {
            if (imc < 21)   return getString(R.string.insuff_ponderale);
            if (imc < 27)   return getString(R.string.poids_normal);
            if (imc < 32)   return getString(R.string.surpoids);
            return getString(R.string.obesite);
        }
    }

    private String getTranche(int age) {
        if (age < 35)  return getString(R.string.tranche_jeune);
        if (age < 65)  return getString(R.string.tranche_moyen);
        return getString(R.string.tranche_age);
    }

    // ---- RAZ ----
    private void confirmerRAZ() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.dialog_raz_message)
                .setNegativeButton(R.string.annuler, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> razChamps())
                .show();
    }

    private void razChamps() {
        spinnerGenre.setSelection(0);
        editDateNaissance.setText("");
        editPoids.setText("");
        editTaille.setText("");
        radioCentimetre.setChecked(true);
        checkBoxAffichage.setChecked(false);
        textResultat.setText(R.string.texte_defaut_resultat);
        imcCalcule = -1;
        
        // Supprimer la sauvegarde
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().remove(IMCdata).apply();
    }
}
