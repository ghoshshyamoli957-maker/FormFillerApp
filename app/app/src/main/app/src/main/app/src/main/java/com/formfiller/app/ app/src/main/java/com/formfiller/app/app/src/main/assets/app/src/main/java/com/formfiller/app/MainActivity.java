package com.formfiller.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {

    static class Tab {
        WebView w;
        String custom, pending, title = "";
    }

    final List<Tab> tabs = new ArrayList<>();
    int cur = -1;
    SharedPreferences sp;
    String helperJs = "";
    FrameLayout holder;
    LinearLayout tabRow;
    EditText urlBox;
    Button bBack, bFwd;
    ActivityResultLauncher<String[]> picker;

    public class Bridge {
        @JavascriptInterface public String load(String k) { return sp.getString("js_" + k, ""); }
        @JavascriptInterface public void save(String k, String v) { sp.edit().putString("js_" + k, v).apply(); }
        @JavascriptInterface public String getData() {
            return sp.getString("data", "{\"headers\":[],\"rows\":[]}");
        }
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("ff", MODE_PRIVATE);
        try {
            InputStream in = getAssets().open("helper.js");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            helperJs = bo.toString("UTF-8");
        } catch (Exception e) { }
        picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) importFile(uri);
        });
        buildUi();
        boolean ok = false;
        try {
            JSONArray a = new JSONArray(sp.getString("tabs", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Tab t = addTab(o.optString("u", ""), false);
                String nm = o.optString("n", "");
                t.custom = nm.isEmpty() ? null : nm;
            }
            if (!tabs.isEmpty()) {
                switchTo(Math.min(sp.getInt("cur", 0), tabs.size() - 1));
                ok = true;
            }
        } catch (Exception e) { }
        if (!ok) addTab("https://www.google.com", true);
    }

    Button mk(String t, View.OnClickListener l) {
        Button x = new Button(this);
        x.setText(t);
        x.setAllCaps(false);
        x.setTextSize(13);
        x.setMinWidth(0);
        x.setMinimumWidth(0);
        x.setMinHeight(dp(40));
        x.setMinimumHeight(dp(40));
        x.setPadding(dp(10), 0, dp(10), 0);
        x.setOnClickListener(l);
        return x;
    }

    void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        HorizontalScrollView hs1 = new HorizontalScrollView(this);
        LinearLayout row1 = new LinearLayout(this);
        bBack = mk("◀ ব্যাক", v -> { WebView w = cw(); if (w != null && w.canGoBack()) w.goBack(); });
        bFwd = mk("ফরওয়ার্ড ▶", v -> { WebView w = cw(); if (w != null && w.canGoForward()) w.goForward(); });
        row1.addView(bBack);
        row1.addView(bFwd);
        row1.addView(mk("কাট", v -> doCut()));
        row1.addView(mk("কপি", v -> doCopy()));
        row1.addView(mk("পেস্ট", v -> doPaste()));
        row1.addView(mk("⟳", v -> { WebView w = cw(); if (w != null) w.reload(); }));
        row1.addView(mk("📂 ফাইল", v -> picker.launch(new String[]{"*/*"})));
        hs1.addView(row1);
        root.addView(hs1);

        LinearLayout tabWrap = new LinearLayout(this);
        HorizontalScrollView hs2 = new HorizontalScrollView(this);
        tabRow = new LinearLayout(this);
        hs2.addView(tabRow);
        tabWrap.addView(hs2, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabWrap.addView(mk("＋", v -> addTab("https://www.google.com", true)));
        root.addView(tabWrap);

        LinearLayout row3 = new LinearLayout(this);
        urlBox = new EditText(this);
        urlBox.setSingleLine(true);
        urlBox.setTextSize(14);
        urlBox.setHint("ঠিকানা বা সার্চ লিখুন");
        urlBox.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlBox.setOnEditorActionListener((v, id, ev) -> { go(); return true; });
        row3.addView(urlBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row3.addView(mk("যান", v -> go()));
        root.addView(row3);

        holder = new FrameLayout(this);
        root.addView(holder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    WebView cw() { return cur >= 0 && cur < tabs.size() ? tabs.get(cur).w : null; }

    Tab addTab(String u, boolean show) {
        final Tab t = new Tab();
        WebView w = new WebView(this);
        t.w = w;
        t.pending = (u == null || u.isEmpty()) ? null : u;
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        w.addJavascriptInterface(new Bridge(), "FF");
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String x = r.getUrl().toString();
                return !(x.startsWith("http") || x.startsWith("about:") || x.startsWith("file:"));
            }
            @Override public void onPageFinished(WebView v, String x) {
                v.evaluateJavascript(helperJs, null);
                String ti = v.getTitle();
                t.title = ti == null ? "" : ti;
                refreshTabs();
            }
            @Override public void doUpdateVisitedHistory(WebView v, String x, boolean r) {
                if (cw() == v && x != null) urlBox.setText(x);
                updateNav();
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView v, boolean d, boolean g, Message m) {
                Tab nt = addTab(null, true);
                WebView.WebViewTransport tr = (WebView.WebViewTransport) m.obj;
                tr.setWebView(nt.w);
                m.sendToTarget();
                return true;
            }
        });
        tabs.add(t);
        if (show) switchTo(tabs.size() - 1);
        else refreshTabs();
        return t;
    }

    void switchTo(int i) {
        if (i < 0 || i >= tabs.size()) return;
        cur = i;
        Tab t = tabs.get(i);
        holder.removeAllViews();
        if (t.w.getParent() != null) ((ViewGroup) t.w.getParent()).removeView(t.w);
        holder.addView(t.w, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (t.pending != null) {
            String p = t.pending;
            t.pending = null;
            t.w.loadUrl(p);
        }
        String cu = t.w.getUrl();
        urlBox.setText(cu == null ? "" : cu);
        refreshTabs();
        updateNav();
    }

    void closeTab(int i) {
        Tab t = tabs.remove(i);
        if (t.w.getParent() != null) ((ViewGroup) t.w.getParent()).removeView(t.w);
        t.w.destroy();
        if (tabs.isEmpty()) {
            cur = -1;
            addTab("https://www.google.com", true);
        } else {
            switchTo(Math.min(i, tabs.size() - 1));
        }
    }

    String label(Tab t) {
        String s = t.custom != null ? t.custom : (t.title.isEmpty() ? "ট্যাব" : t.title);
        return s.length() > 12 ? s.substring(0, 12) + "…" : s;
    }

    void refreshTabs() {
        tabRow.removeAllViews();
        for (int i = 0; i < tabs.size(); i++) {
            final int k = i;
            Button b = mk((i + 1) + ". " + label(tabs.get(i)), v -> switchTo(k));
            boolean sel = i == cur;
            b.setBackgroundColor(sel ? Color.parseColor("#1a73e8") : Color.parseColor("#dddddd"));
            b.setTextColor(sel ? Color.WHITE : Color.BLACK);
            b.setOnLongClickListener(v -> { tabMenu(k); return true; });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            tabRow.addView(b, lp);
        }
    }

    void updateNav() {
        WebView w = cw();
        boolean b = w != null && w.canGoBack(), f = w != null && w.canGoForward();
        bBack.setEnabled(b);
        bBack.setAlpha(b ? 1f : 0.35f);
        bFwd.setEnabled(f);
        bFwd.setAlpha(f ? 1f : 0.35f);
    }

    String urlOf(Tab t) {
        if (t.pending != null) return t.pending;
        String u = t.w.getUrl();
        return u == null ? "" : u;
    }

    void tabMenu(final int i) {
        new AlertDialog.Builder(this).setItems(
                new String[]{"বন্ধ করুন", "নাম বদলান", "এই পেজের কপি (নতুন ট্যাব)"}, (d, which) -> {
                    if (which == 0) closeTab(i);
                    else if (which == 1) rename(i);
                    else addTab(urlOf(tabs.get(i)), true);
                }).show();
    }

    void rename(final int i) {
        final EditText et = new EditText(this);
        et.setText(tabs.get(i).custom == null ? "" : tabs.get(i).custom);
        new AlertDialog.Builder(this).setTitle("ট্যাবের নতুন নাম").setView(et)
                .setPositiveButton("ঠিক আছে", (d, w) -> {
                    String s = et.getText().toString().trim();
                    tabs.get(i).custom = s.isEmpty() ? null : s;
                    refreshTabs();
                }).setNegativeButton("বাতিল", null).show();
    }

    void go() {
        WebView w = cw();
        String q = urlBox.getText().toString().trim();
        if (w == null || q.isEmpty()) return;
        String u;
        if (q.contains("://")) u = q;
        else if (q.contains(".") && !q.contains(" ")) u = "https://" + q;
        else {
            try { u = "https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8"); }
            catch (Exception e) { return; }
        }
        w.loadUrl(u);
        w.requestFocus();
    }

    String unq(String r) {
        if (r == null || r.equals("null")) return "";
        try {
            Object o = new JSONTokener(r).nextValue();
            return o == null || o == JSONObject.NULL ? "" : o.toString();
        } catch (Exception e) { return ""; }
    }

    void clip(String s) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("t", s));
    }

    void doCopy() {
        WebView w = cw();
        if (w == null) return;
        w.evaluateJavascript(
                "(function(){var e=document.activeElement;"
                + "if(e&&e.value!==undefined&&e.selectionStart!=null&&e.selectionEnd>e.selectionStart)"
                + "return e.value.substring(e.selectionStart,e.selectionEnd);"
                + "return window.getSelection().toString();})()",
                r -> {
                    String s = unq(r);
                    if (s.isEmpty()) toast("আগে লেখা সিলেক্ট করুন");
                    else { clip(s); toast("কপি হয়েছে"); }
                });
    }

    void doCut() {
        WebView w = cw();
        if (w == null) return;
        w.evaluateJavascript(
                "(function(){var e=document.activeElement;"
                + "if(e&&e.value!==undefined&&e.selectionStart!=null&&e.selectionEnd>e.selectionStart){"
                + "var s=e.value.substring(e.selectionStart,e.selectionEnd);e.setRangeText('');"
                + "e.dispatchEvent(new Event('input',{bubbles:true}));return s;}return '';})()",
                r -> {
                    String s = unq(r);
                    if (s.isEmpty()) toast("ঘরের ভেতরে লেখা সিলেক্ট করুন");
                    else { clip(s); toast("কাট হয়েছে"); }
                });
    }

    void doPaste() {
        WebView w = cw();
        if (w == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (!cm.hasPrimaryClip() || cm.getPrimaryClip().getItemCount() == 0) { toast("ক্লিপবোর্ড খালি"); return; }
        CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
        if (cs == null || cs.length() == 0) { toast("ক্লিপবোর্ড খালি"); return; }
        String q = JSONObject.quote(cs.toString());
        w.evaluateJavascript(
                "(function(t){var e=document.activeElement;"
                + "if(e&&e.value!==undefined){try{e.setRangeText(t,e.selectionStart,e.selectionEnd,'end');}"
                + "catch(x){e.value=e.value+t;}"
                + "e.dispatchEvent(new Event('input',{bubbles:true}));"
                + "e.dispatchEvent(new Event('change',{bubbles:true}));}"
                + "else{document.execCommand('insertText',false,t);}})(" + q + ")", null);
    }

    void importFile(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            List<String[]> all = XlsxReader.read(in);
            in.close();
            if (all.isEmpty()) { toast("ফাইলে কোনো ডাটা নেই"); return; }
            JSONArray h = new JSONArray(), rs = new JSONArray();
            for (String c : all.get(0)) h.put(c);
            for (int i = 1; i < all.size(); i++) {
                JSONArray r = new JSONArray();
                for (String c : all.get(i)) r.put(c);
                rs.put(r);
            }
            JSONObject o = new JSONObject();
            o.put("headers", h);
            o.put("rows", rs);
            sp.edit().putString("data", o.toString()).apply();
            toast("ইমপোর্ট হয়েছে: " + (all.size() - 1) + "টি রো");
        } catch (Exception e) {
            toast("ফাইল পড়া যায়নি: " + e.getMessage());
        }
    }

    @Override protected void onPause() {
        super.onPause();
        try {
            JSONArray a = new JSONArray();
            for (Tab t : tabs) {
                JSONObject o = new JSONObject();
                o.put("u", urlOf(t));
                o.put("n", t.custom == null ? "" : t.custom);
                a.put(o);
            }
            sp.edit().putString("tabs", a.toString()).putInt("cur", cur).apply();
        } catch (Exception e) { }
    }

    @Override public void onBackPressed() {
        WebView w = cw();
        if (w != null && w.canGoBack()) w.goBack();
        else super.onBackPressed();
    }
    }
