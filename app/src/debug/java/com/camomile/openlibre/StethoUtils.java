package com.camomile.openlibre;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.io.File;
import java.util.regex.Pattern;

class StethoUtils {

   static void install(Application application, File dataPath){
        // debugging tool
        Stetho.initialize(
                Stetho.newInitializerBuilder(application)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(application))
                        .enableWebKitInspector(RealmInspectorModulesProvider
                                .builder(application)
                                .withFolder(dataPath)
                                .withMetaTables()
                                .withLimit(1000000)
                                .databaseNamePattern(Pattern.compile(".+\\.realm"))
                                .build())
                        .build());
   }
}

