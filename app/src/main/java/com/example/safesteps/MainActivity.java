package com.example.safesteps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String PREFS = "safe_steps";
    public static final String PASSWORD = "password";
    public static final String ENABLED = "enabled";
    public static final String SHOW_BLOCKED_SCREEN = "show_blocked_screen";
    private static final String DEFAULT_PASSWORD = "1234";

    private TextView state;
    private Button toggle;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra(SHOW_BLOCKED_SCREEN, false)) {
            showBlockedPage();
        } else {
            buildMain();
        }
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); return t;
    }

    private void buildMain() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(244,247,250));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this); icon.setImageResource(com.example.safesteps.R.drawable.ic_guard);
        header.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));
        Space gap = new Space(this); header.addView(gap, new LinearLayout.LayoutParams(dp(12), 1));
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text("Safe Steps", 25, Color.rgb(32,55,68)); name.setGravity(Gravity.RIGHT);
        TextView tagline = text("צעדים בטוחים בשבילך", 14, Color.rgb(92,111,122)); tagline.setGravity(Gravity.RIGHT);
        names.addView(name); names.addView(tagline);
        header.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView menu = text("⋮", 30, Color.rgb(32,55,68)); menu.setOnClickListener(v -> showMenu(menu));
        header.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(58)));
        root.addView(header);

        Space s1 = new Space(this); root.addView(s1, new LinearLayout.LayoutParams(1, dp(26)));

        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(22),dp(22),dp(22),dp(22)); card.setGravity(Gravity.CENTER_HORIZONTAL); card.setBackground(bg(Color.WHITE, 24));
        TextView title = text("מצב ההגנה", 18, Color.rgb(45,65,76)); card.addView(title);
        state = text("", 24, Color.rgb(49,94,120)); card.addView(state, new LinearLayout.LayoutParams(-1, dp(52)));
        toggle = new Button(this); toggle.setText("הפעל / כבה הגנה"); toggle.setTextSize(17); toggle.setTextColor(Color.WHITE); toggle.setBackground(bg(Color.rgb(49,94,120), 18)); card.addView(toggle, new LinearLayout.LayoutParams(-1, dp(54)));
        root.addView(card);

        Space s2 = new Space(this); root.addView(s2, new LinearLayout.LayoutParams(1, dp(18)));
        Button acc = new Button(this); acc.setText("פתיחת הגדרות נגישות"); acc.setTextSize(16); acc.setBackground(bg(Color.WHITE, 18)); acc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(acc, new LinearLayout.LayoutParams(-1, dp(52)));

        toggle.setOnClickListener(v -> toggleProtection());
        updateState(); setContentView(root);
    }

    private void showMenu(View anchor) {
        PopupMenu p = new PopupMenu(this, anchor, Gravity.END);
        p.getMenu().add("שינוי סיסמת מנהל"); p.getMenu().add("אודות");
        p.setOnMenuItemClickListener(item -> { if (item.getTitle().toString().startsWith("שינוי")) showChangePassword(); else showAbout(); return true; });
        p.show();
    }

    private void toggleProtection() {
        final EditText pass = new EditText(this); pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); pass.setHint("סיסמת מנהל");
        AlertDialog d = new AlertDialog.Builder(this).setTitle(isEnabled()?"כיבוי ההגנה":"הפעלת ההגנה").setMessage("יש להזין את סיסמת המנהל.").setView(pass).setNegativeButton("ביטול", null).setPositiveButton("אישור", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> { if (!pass.getText().toString().equals(getPassword())) { pass.setError("סיסמה שגויה"); return; } getSharedPreferences(PREFS,0).edit().putBoolean(ENABLED,!isEnabled()).apply(); updateState(); d.dismiss(); })); d.show();
    }

    private void showChangePassword() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(24),0,dp(24),0);
        EditText current = field("הסיסמה הנוכחית"); EditText next = field("סיסמה חדשה"); EditText confirm = field("אימות הסיסמה החדשה"); box.addView(current); box.addView(next); box.addView(confirm);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("שינוי סיסמת מנהל").setView(box).setNegativeButton("ביטול", null).setPositiveButton("שמירה", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> { String a=current.getText().toString(), b=next.getText().toString(), c=confirm.getText().toString(); if(!a.equals(getPassword())){current.setError("סיסמה שגויה");return;} if(b.trim().isEmpty()){next.setError("יש להזין סיסמה חדשה");return;} if(!b.equals(c)){confirm.setError("הסיסמאות אינן זהות");return;} getSharedPreferences(PREFS,0).edit().putString(PASSWORD,b).apply(); Toast.makeText(this,"הסיסמה שונתה בהצלחה",Toast.LENGTH_SHORT).show(); d.dismiss(); })); d.show();
    }

    private EditText field(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); e.setPadding(0,dp(8),0,dp(8)); return e; }

    private void showAbout() {
        new AlertDialog.Builder(this).setTitle("אודות Safe Steps").setMessage("Safe Steps\nצעדים בטוחים בשבילך\n\n© כל הזכויות שמורות לישראל מויאל.\nליצירת קשר פנו במייל: inm758595@gmail.com").setPositiveButton("סגירה", null).show();
    }

    private void showBlockedPage() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(dp(30),dp(30),dp(30),dp(30)); root.setBackgroundColor(Color.rgb(244,247,250));
        ImageView icon = new ImageView(this); icon.setImageResource(R.drawable.ic_guard); root.addView(icon,new LinearLayout.LayoutParams(dp(120),dp(120)));
        TextView h = text("הפעולה חסומה.",28,Color.rgb(32,55,68)); h.setTypeface(null,1); root.addView(h,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView msg = text("מכשיר זה מוגן על ידי מערכת Safe Steps.",18,Color.rgb(76,92,102)); msg.setGravity(Gravity.CENTER); root.addView(msg,new LinearLayout.LayoutParams(-1,dp(65)));
        TextView brand = text("Safe Steps",23,Color.rgb(49,94,120)); brand.setTypeface(null,1); root.addView(brand);
        TextView tag = text("צעדים בטוחים בשבילך",14,Color.rgb(92,111,122)); root.addView(tag);
        Space sp=new Space(this); root.addView(sp,new LinearLayout.LayoutParams(1,dp(30),1));
        Button back=new Button(this); back.setText("חזור"); back.setTextColor(Color.WHITE); back.setTextSize(17); back.setBackground(bg(Color.rgb(49,94,120),18)); back.setOnClickListener(v -> { Intent i=new Intent(Intent.ACTION_MAIN); i.addCategory(Intent.CATEGORY_HOME); i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); finish(); }); root.addView(back,new LinearLayout.LayoutParams(-1,dp(54)));
        setContentView(root);
        new android.os.Handler().postDelayed(() -> { if(!isFinishing()){ Intent i=new Intent(Intent.ACTION_MAIN); i.addCategory(Intent.CATEGORY_HOME); i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); } }, 2600);
    }

    private String getPassword(){ return getSharedPreferences(PREFS,0).getString(PASSWORD,DEFAULT_PASSWORD); }
    private boolean isEnabled(){ return getSharedPreferences(PREFS,0).getBoolean(ENABLED,false); }
    private void updateState(){ if(state!=null) state.setText(isEnabled()?"ההגנה פעילה":"ההגנה כבויה"); }
}
