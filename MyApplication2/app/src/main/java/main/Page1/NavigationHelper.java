package main.Page1;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import main.IMC.IMCMain;
import main.Page1.MainMusic;

public class NavigationHelper {

    public static void ouvrirMusique(Context context) {
        Intent intent = new Intent(context, MainMusic.class);
        context.startActivity(intent);
    }
    public static void ouvrirIMC(Context context) {
        Intent intent = new Intent(context, IMCMain.class);
        context.startActivity(intent);
    }
}