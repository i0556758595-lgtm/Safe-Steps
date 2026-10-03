package com.example.safesteps;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

public class GuardAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler();
    private long lastBlock = 0;
    private boolean blocking = false;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || !getSharedPreferences(MainActivity.PREFS,0).getBoolean(MainActivity.ENABLED,false)) return;
        CharSequence p = event.getPackageName(); if (p == null) return;
        String pkg = p.toString(); if (pkg.equals(getPackageName())) return;
        if (isForbiddenPackage(pkg) || isForbiddenAccessibilityScreen(event) || isSafeStepsDetails(event)) blockNow();
    }

    private boolean isForbiddenPackage(String pkg) {
        return pkg.equals("com.android.packageinstaller") || pkg.equals("com.google.android.packageinstaller") || pkg.equals("com.android.permissioncontroller");
    }

    private boolean isForbiddenAccessibilityScreen(AccessibilityEvent e) {
        String pkg = String.valueOf(e.getPackageName()).toLowerCase(Locale.ROOT);
        if (!pkg.equals("com.android.settings")) return false;
        String cls = String.valueOf(e.getClassName()).toLowerCase(Locale.ROOT);
        if (cls.contains("accessibility")) return true;
        String text = collectEventText(e).toLowerCase(Locale.ROOT);
        return text.contains("נגישות") || text.contains("accessibility") || cls.contains("installedaccessibility") || cls.contains("accessibilitysettings");
    }

    private boolean isSafeStepsDetails(AccessibilityEvent e) {
        String pkg = String.valueOf(e.getPackageName());
        if (!pkg.equals("com.android.settings")) return false;
        String text = collectEventText(e).toLowerCase(Locale.ROOT);
        String cls = String.valueOf(e.getClassName()).toLowerCase(Locale.ROOT);
        return (text.contains("safe steps") || text.contains("safesteps")) && (text.contains("עצירה") || text.contains("השבת") || text.contains("נקה נתונים") || text.contains("הסרה") || text.contains("force stop") || text.contains("uninstall") || cls.contains("appdetails"));
    }

    private String collectEventText(AccessibilityEvent e) {
        StringBuilder b=new StringBuilder();
        for(CharSequence c:e.getText()) if(c!=null) b.append(' ').append(c);
        CharSequence d=e.getContentDescription(); if(d!=null) b.append(' ').append(d);
        return b.toString();
    }

    private void blockNow() {
        long now=System.currentTimeMillis(); if (blocking || now-lastBlock<900) return; lastBlock=now; blocking=true;
        performGlobalAction(GLOBAL_ACTION_BACK);
        handler.postDelayed(() -> {
            try {
                Intent i=new Intent(this,MainActivity.class);
                i.putExtra(MainActivity.SHOW_BLOCKED_SCREEN,true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            } finally { blocking=false; }
        }, 20);
    }

    @Override public void onInterrupt() { blocking=false; }
}
