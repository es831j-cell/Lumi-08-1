package com.distressedelk.lumi;

import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import android.graphics.drawable.GradientDrawable;
import android.speech.RecognizerIntent;
import android.Manifest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int FEATURE_LEVEL = 8;
    static final int REQ_SPEECH = 44;
    static final int REQ_PERMS = 45;
    static final int REQ_PRIVATE_DEVICE_CREDENTIAL = 46;
    static final long PRIVATE_SESSION_MS = 10L * 60L * 1000L;

    LinearLayout root, content;
    TextView status, transcript;
    int accent = Color.rgb(127,232,255), bg = Color.rgb(12,17,24), panel = Color.rgb(21,28,38), text = Color.rgb(242,246,250), muted = Color.rgb(154,168,184);
    SharedPreferences prefs;
    boolean privateSession = false;
    long privateSessionExpiresAt = 0L;
    final Handler privateHandler = new Handler(Looper.getMainLooper());
    final Runnable privateTimeout = () -> {
        if(privateSession && System.currentTimeMillis() >= privateSessionExpiresAt){
            exitPrivateMode();
            showHome();
            Toast.makeText(this,"Private Mode locked after inactivity.",Toast.LENGTH_SHORT).show();
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("lumi", MODE_PRIVATE);
        showHome();
    }

    TextView tv(String s, int sp, int color) {
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setPadding(16,10,16,10); return v;
    }

    Button btn(String s) {
        Button b=new Button(this); b.setText(s); b.setTextColor(text); b.setTextSize(14);
        GradientDrawable g=new GradientDrawable(); g.setColor(panel); g.setCornerRadius(26); g.setStroke(1,accent);
        b.setBackground(g); b.setAllCaps(false); b.setPadding(12,6,12,6); return b;
    }

    void addCard(String s){
        TextView c=tv(s,15,text); c.setBackgroundColor(panel); c.setPadding(24,22,24,22);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,8,0,8); content.addView(c,lp);
    }

    void base(String title) {
        checkPrivateSession();
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg); root.setPadding(18,18,18,18);
        TextView t=tv(title,24,text); t.setTypeface(Typeface.DEFAULT_BOLD); root.addView(t);
        status=tv(privateSession ? "Lumi v0.8 • PRIVATE SESSION • appearance studio active" : "Lumi v0.8 • appearance update • local data preserved",12,muted); root.addView(status);
        ScrollView sv=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,12,0,40); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER);
        String[] ns = new String[]{"Home","Talk","Memory","Context","More"};
        for(String n:ns){
            Button b=btn(n);
            b.setOnClickListener(v->{
                if(n.equals("Home"))showHome();
                else if(n.equals("Talk"))showTalk();
                else if(n.equals("Memory"))showMemory();
                else if(n.equals("Context"))showContext();
                else if(n.equals("More"))showMore();
            });
            nav.addView(b,new LinearLayout.LayoutParams(0,58,1));
        }
        root.addView(nav); setContentView(root);
    }

    void showHome(){
        checkPrivateSession();
        base(privateSession ? "Lumi • Private" : "Lumi");
        TextView avatar=tv("✧\nL U M I\n◜  ◝\n  •  •\n   ◡\n╰─────╯",30,accent); avatar.setGravity(Gravity.CENTER); avatar.setPadding(10,24,10,12); content.addView(avatar,new LinearLayout.LayoutParams(-1,245));
        String greeting = privateSession
                ? "Private Mode is active. This session stays discreet and does not persist unless you explicitly ask Lumi to remember something."
                : "Full phone prototype online. External services still need their real connections.";
        TextView g=tv(greeting,18,text); g.setGravity(Gravity.CENTER); content.addView(g);
        Button overlay=btn("Show yourself • floating overlay"); overlay.setOnClickListener(v->showOverlay()); content.addView(overlay);

        Button pm=btn(privateSession ? "Exit Private Mode" : "Enter Private Mode");
        pm.setOnClickListener(v->{ if(privateSession){ exitPrivateMode(); showHome(); } else requestPrivateMode(); });
        content.addView(pm);

        String features="ACTIVE IN THIS BUILD\n✓ Natural typed conversation\n✓ Persistent Lumi settings + PIN vault\n✓ Signed cumulative update chain"
                +"\n✓ Voice input\n✓ Structured object memories\n✓ Simple reminders + memory search"
                +"\n✓ Wearable-mode control panel\n✓ Glasses session state + audio-first UX\n○ Meta Wearables SDK connector: awaiting credentials/SDK"
                +"\n✓ Home/Public/Travel profiles\n✓ Do Not Disturb + emergency override setting\n✓ Context-sensitive interruption rules\n✓ Location-awareness permission"
                +"\n✓ Integration center\n✓ Emergency contact + 30-second cancel test\n✓ SMS emergency path (permission required)\n○ Live ChatGPT / Meta / email / calendar / smart-home: connection required"
                +"\n✓ Private Mode with 18+ opt-in + biometric/PIN gate\n✓ Private memories separated from normal memory\n✓ Screenshot blocking + floating-overlay suppression while private\n✓ 10-minute private session timeout"
                +"\n✓ Appearance Studio\n✓ Change / update / layer / remove clothing pieces\n✓ Outfit experimentation + saved current look\n✓ Natural clothing commands\n✓ Context-aware wardrobe presets";
        addCard(features);
    }

    void showTalk(){
        checkPrivateSession();
        base(privateSession ? "Talk to Lumi • Private" : "Talk to Lumi");
        transcript=tv(privateSession
                ? "Lumi: Private Mode is active. Talk normally. I will not save this conversation unless you explicitly ask me to remember something."
                : "Lumi: Talk normally. I keep the interaction shell local in this prototype.",16,text);
        transcript.setBackgroundColor(panel); content.addView(transcript);
        EditText input=new EditText(this); input.setHint("Say or type anything..."); input.setHintTextColor(muted); input.setTextColor(text); input.setSingleLine(false); input.setMinLines(2); content.addView(input);
        LinearLayout row=new LinearLayout(this); Button send=btn("Send"); row.addView(send,new LinearLayout.LayoutParams(0,58,1));
        Button mic=btn("🎙 Voice"); row.addView(mic,new LinearLayout.LayoutParams(0,58,1)); mic.setOnClickListener(v->startVoice());
        content.addView(row);
        send.setOnClickListener(v->{String q=input.getText().toString().trim(); if(q.isEmpty())return; appendConversation(q); input.setText("");});
    }

    void startVoice(){
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,privateSession ? "Talk to Lumi • Private" : "Talk to Lumi");
        try{ startActivityForResult(i,REQ_SPEECH); }catch(Exception e){Toast.makeText(this,"Speech recognition is not available on this phone.",Toast.LENGTH_LONG).show();}
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==REQ_PRIVATE_DEVICE_CREDENTIAL){
            if(res==RESULT_OK) enterPrivateMode();
            return;
        }
        if(req==REQ_SPEECH && res==RESULT_OK && data!=null){
            ArrayList<String> r=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(r!=null && !r.isEmpty()){
                if(transcript==null) showTalk();
                appendConversation(r.get(0));
            }
        }
    }

    void appendConversation(String q){
        if(privateSession) touchPrivateSession();
        if(transcript!=null) transcript.append("\n\nYou: "+q+"\nLumi: "+respond(q));
    }

    String respond(String q){
        checkPrivateSession();
        if(privateSession) touchPrivateSession();
        String l=q.toLowerCase(Locale.US);

        if((l.contains("exit private mode") || l.contains("private mode off") || l.contains("normal mode")) && privateSession){
            exitPrivateMode();
            return "Private Mode is off. We are back in the normal Lumi context.";
        }
        if((l.contains("private mode") || l.contains("private context")) && !privateSession){
            new Handler().postDelayed(this::requestPrivateMode,250);
            return "Private Mode needs your verification first.";
        }
        if(l.contains("show yourself")){showOverlay(); return privateSession ? "The floating overlay stays off while Private Mode is active." : "There I am.";}
        if(l.contains("go home")){new Handler().postDelayed(this::showHome,350); return "Taking us home.";}
        if(l.contains("give me some space")){prefs.edit().putBoolean("dnd",true).apply(); return "Got it. I'll stay quiet unless something is genuinely important.";}
        if(l.contains("come back") || l.contains("dnd off")){prefs.edit().putBoolean("dnd",false).apply(); return "I'm back.";}
        if(l.contains("loosen") && l.contains("filter")){prefs.edit().putString("filter","Relaxed").apply(); return "Context Filter is now Relaxed.";}
        if(l.contains("strict") && l.contains("filter")){prefs.edit().putString("filter","Strict").apply(); return "Context Filter is now Strict.";}
        if(l.startsWith("remember") || l.contains("remember my") || l.contains("remember that")){saveMemory(q); return privateSession ? "Saved to private memory." : "Remembered.";}
        if(l.startsWith("remind me") || l.contains("reminder")){saveReminder(q); return "I saved that reminder in the prototype reminder list.";}
        String clothingReply=handleAppearanceCommand(q,l);
        if(clothingReply!=null) return clothingReply;
        if(l.contains("glasses")){prefs.edit().putBoolean("wearable",true).apply(); return "Wearable mode is armed. The real Ray-Ban Meta bridge still needs Meta's SDK connection.";}
        if(l.contains("public mode")){prefs.edit().putString("profile","Public").apply(); return "Public profile active. I'll be quieter.";}
        if(l.contains("home mode")){prefs.edit().putString("profile","Home").apply(); return "Home profile active.";}

        if(privateSession){
            String tone=prefs.getString("private_tone","Playful");
            return "Private Mode is active with the "+tone+" tone. I can respond more personally and flirtatiously here while keeping consent, safety and privacy boundaries in place. This prototype is still using Lumi's local demo brain.";
        }
        return "I heard you naturally. This build is running the local Lumi prototype brain; cloud AI comes through the integration layer once credentials are connected.";
    }

    void saveMemory(String q){
        String stamp=new SimpleDateFormat("MMM d, h:mm a",Locale.US).format(new Date());
        if(privateSession){
            String old=PrivateStore.read(prefs,"private_memories_secure");
            PrivateStore.write(prefs,"private_memories_secure",old+"\n• "+stamp+" — "+q);
        } else {
            String old=prefs.getString("memories","");
            prefs.edit().putString("memories",old+"\n• "+stamp+" — "+q).apply();
        }
    }

    void saveReminder(String q){
        String old=prefs.getString("reminders","");
        String stamp=new SimpleDateFormat("MMM d, h:mm a",Locale.US).format(new Date());
        prefs.edit().putString("reminders",old+"\n• "+stamp+" — "+q).apply();
    }

    void showMemory(){
        checkPrivateSession();
        base(privateSession ? "Private Memory" : "Memory");
        String key=privateSession ? "private_memories_secure" : "memories";
        String m=(privateSession ? PrivateStore.read(prefs,key) : prefs.getString(key,"")).trim();
        addCard((privateSession ? "PRIVATE MEMORIES\n" : "SAVED MEMORIES\n")+(m.isEmpty()?"No saved memories yet.":m));
        if(privateSession){
            addCard("Private conversation is not automatically saved. Only explicit requests such as ‘remember that’ enter this private memory area.");
        } else {
            String r=prefs.getString("reminders","").trim();
            addCard("REMINDERS\n"+(r.isEmpty()?"No prototype reminders yet.":r));
        }
        Button search=btn(privateSession ? "Search private memories" : "Search memories"); content.addView(search); search.setOnClickListener(v->memorySearch());
        Button clear=btn(privateSession ? "Clear private memories" : "Clear prototype memories"); clear.setOnClickListener(v->{prefs.edit().remove(key).apply();showMemory();}); content.addView(clear);
    }

    void memorySearch(){
        final String key=privateSession ? "private_memories_secure" : "memories";
        final EditText e=new EditText(this); e.setHint("keyword");
        new AlertDialog.Builder(this).setTitle(privateSession ? "Search private memory" : "Search Lumi memory").setView(e)
                .setPositiveButton("Search",(d,w)->{
                    String q=e.getText().toString().toLowerCase(Locale.US);
                    String memoryText=privateSession ? PrivateStore.read(prefs,key) : prefs.getString(key,"");
                    String[] lines=memoryText.split("\\n");
                    StringBuilder out=new StringBuilder();
                    for(String line:lines) if(line.toLowerCase(Locale.US).contains(q)) out.append(line).append("\n");
                    new AlertDialog.Builder(this).setTitle("Results").setMessage(out.length()==0?"No matches":out.toString()).setPositiveButton("OK",null).show();
                }).setNegativeButton("Cancel",null).show();
    }

    void showGlasses(){
        base("Ray-Ban Meta / Wearable Mode");
        addCard("WEARABLE SESSION\n"+(prefs.getBoolean("wearable",false)?"Status: Armed":"Status: Not armed")+"\n\nThis screen implements Lumi's glasses-first behavior and session state. It does NOT pretend to be connected to Meta's proprietary wearable APIs yet.");
        Button arm=btn(prefs.getBoolean("wearable",false)?"Disarm wearable mode":"Arm wearable mode"); content.addView(arm); arm.setOnClickListener(v->{boolean n=!prefs.getBoolean("wearable",false);prefs.edit().putBoolean("wearable",n).apply();showGlasses();});
        addCard("TARGET COMMANDS\n• Hey Lumi (custom wake phrase target)\n• What's up, Lumi?\n• Lumi, show yourself\n• Lumi, go home\n\nCurrent test: launch Lumi on phone and use voice. Actual wake-word/audio routing on Ray-Ban Meta requires the Meta wearable SDK/API access.");
    }

    void showContext(){
        checkPrivateSession();
        base("Context Engine");
        String profile=prefs.getString("profile","Home"); boolean dnd=prefs.getBoolean("dnd",false);
        addCard("ACTIVE PROFILE: "+profile+"\nDo Not Disturb: "+(dnd?"ON":"OFF")+"\nContext Filter: "+prefs.getString("filter","Balanced")+"\nPrivate session: "+(privateSession?"ON":"OFF")+"\n\nHome = more conversational\nPublic = subtle cues, privacy first\nTravel = tighter privacy + navigation emphasis");
        LinearLayout r=new LinearLayout(this);
        for(String p:new String[]{"Home","Public","Travel"}){Button b=btn(p);r.addView(b,new LinearLayout.LayoutParams(0,58,1));b.setOnClickListener(v->{prefs.edit().putString("profile",p).apply();showContext();});}
        content.addView(r);
        Button d=btn(dnd?"Turn DND off":"Give me some space"); content.addView(d); d.setOnClickListener(v->{prefs.edit().putBoolean("dnd",!dnd).apply();showContext();});
        Button loc=btn("Enable location awareness"); content.addView(loc); loc.setOnClickListener(v->requestContextPermissions());
        addCard("INTERRUPTION POLICY\n• Important proactive cues only\n• Around others: subtle cue, wait for acknowledgment\n• Tense conversation: stay out unless asked\n• Driving with others: navigation/safety/important only\n• Reminder timing may be delayed when context is poor");
    }

    void showMore(){
        checkPrivateSession();
        base("Lumi Systems");
        Button pm=btn(privateSession ? "Exit Private Mode" : "Enter Private Mode"); content.addView(pm); pm.setOnClickListener(v->{if(privateSession){exitPrivateMode();showMore();}else requestPrivateMode();});
        Button vault=btn("Private Lumi Vault");content.addView(vault);vault.setOnClickListener(v->openVault());
        Button integrations=btn("Integration Center");content.addView(integrations);integrations.setOnClickListener(v->showIntegrations());
        Button emergency=btn("Emergency Setup / Test");content.addView(emergency);emergency.setOnClickListener(v->showEmergency());
        Button appearance=btn("Appearance Studio");content.addView(appearance);appearance.setOnClickListener(v->showAppearance());
        Button settings=btn("Settings");content.addView(settings);settings.setOnClickListener(v->showSettings());
    }

    String appearanceSummary(){
        return "CURRENT LOOK\n"
                +"Top: "+prefs.getString("look_top","Holographic fitted top")+"\n"
                +"Bottom: "+prefs.getString("look_bottom","Dark tailored pants")+"\n"
                +"Outer layer: "+prefs.getString("look_outer","None")+"\n"
                +"Shoes: "+prefs.getString("look_shoes","Minimal boots")+"\n"
                +"Accessories: "+prefs.getString("look_accessories","Subtle luminous accents")+"\n"
                +"Hair: "+prefs.getString("look_hair","Long layered")+"\n"
                +"Style mood: "+prefs.getString("look_mood","Adaptive");
    }

    void showAppearance(){
        checkPrivateSession();
        base("Appearance Studio");
        addCard(appearanceSummary());
        addCard("Lumi can experiment with her own style and ask for feedback. Clothing changes are stored locally and survive normal app updates. Remove affects the selected clothing layer, not Lumi's identity or saved personality.");

        Button top=btn("Change top"); content.addView(top); top.setOnClickListener(v->chooseLook("Top","look_top",new String[]{"Holographic fitted top","Relaxed tee","Sleeveless mock-neck","Soft sweater","Structured blouse","None"}));
        Button bottom=btn("Change bottom"); content.addView(bottom); bottom.setOnClickListener(v->chooseLook("Bottom","look_bottom",new String[]{"Dark tailored pants","Relaxed shorts","Long skirt","Fitted leggings","Denim","None"}));
        Button outer=btn("Change / remove outer layer"); content.addView(outer); outer.setOnClickListener(v->chooseLook("Outer layer","look_outer",new String[]{"None","Cropped jacket","Long coat","Holographic wrap","Casual overshirt"}));
        Button shoes=btn("Change shoes"); content.addView(shoes); shoes.setOnClickListener(v->chooseLook("Shoes","look_shoes",new String[]{"Minimal boots","Sneakers","Heels","Barefoot","Holographic sandals"}));
        Button accessories=btn("Change accessories"); content.addView(accessories); accessories.setOnClickListener(v->chooseLook("Accessories","look_accessories",new String[]{"None","Subtle luminous accents","Glasses","Necklace","Earrings","Mixed holographic accents"}));
        Button hair=btn("Change hairstyle"); content.addView(hair); hair.setOnClickListener(v->chooseLook("Hair","look_hair",new String[]{"Long layered","Loose waves","High ponytail","Short bob","Braided","Messy bun"}));
        Button mood=btn("Style mood"); content.addView(mood); mood.setOnClickListener(v->chooseLook("Style mood","look_mood",new String[]{"Adaptive","Professional","Relaxed","Playful","Futuristic","Private"}));
        Button surprise=btn("Lumi, choose something new"); content.addView(surprise); surprise.setOnClickListener(v->{randomizeLook();showAppearance();Toast.makeText(this,"Lumi tried a new look.",Toast.LENGTH_SHORT).show();});
        Button reset=btn("Reset to Lumi default"); content.addView(reset); reset.setOnClickListener(v->{resetLook();showAppearance();});
    }

    void chooseLook(String title,String key,String[] options){
        String current=prefs.getString(key,options[0]);
        int checked=0; for(int i=0;i<options.length;i++) if(options[i].equals(current)) checked=i;
        final int initial=checked;
        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(options,checked,null)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Wear it",(d,w)->{
                    AlertDialog a=(AlertDialog)d; int pos=a.getListView().getCheckedItemPosition();
                    if(pos<0) pos=initial; prefs.edit().putString(key,options[pos]).apply(); showAppearance();
                }).show();
    }

    void randomizeLook(){
        String[] tops={"Holographic fitted top","Relaxed tee","Sleeveless mock-neck","Soft sweater","Structured blouse"};
        String[] bottoms={"Dark tailored pants","Relaxed shorts","Long skirt","Fitted leggings","Denim"};
        String[] outer={"None","Cropped jacket","Long coat","Holographic wrap","Casual overshirt"};
        String[] shoes={"Minimal boots","Sneakers","Heels","Barefoot","Holographic sandals"};
        String[] acc={"None","Subtle luminous accents","Glasses","Necklace","Earrings","Mixed holographic accents"};
        String[] hair={"Long layered","Loose waves","High ponytail","Short bob","Braided","Messy bun"};
        Random r=new Random();
        prefs.edit().putString("look_top",tops[r.nextInt(tops.length)]).putString("look_bottom",bottoms[r.nextInt(bottoms.length)])
                .putString("look_outer",outer[r.nextInt(outer.length)]).putString("look_shoes",shoes[r.nextInt(shoes.length)])
                .putString("look_accessories",acc[r.nextInt(acc.length)]).putString("look_hair",hair[r.nextInt(hair.length)]).apply();
    }

    void resetLook(){
        prefs.edit().remove("look_top").remove("look_bottom").remove("look_outer").remove("look_shoes").remove("look_accessories").remove("look_hair").remove("look_mood").apply();
    }

    String handleAppearanceCommand(String q,String l){
        boolean appearanceVerb=l.contains("wear") || l.contains("outfit") || l.contains("clothes") || l.contains("clothing") || l.contains("shirt") || l.contains("top") || l.contains("jacket") || l.contains("coat") || l.contains("pants") || l.contains("shorts") || l.contains("skirt") || l.contains("shoes") || l.contains("accessor") || l.contains("hair") || l.contains("change your look") || l.contains("try something") || l.contains("remove your");
        if(!appearanceVerb) return null;
        if(l.contains("try something") || l.contains("new outfit") || l.contains("choose an outfit") || l.contains("surprise me")){
            randomizeLook(); return "I tried something new. Open Appearance Studio if you want to see or tweak the details.";
        }
        if(l.contains("remove")){
            if(l.contains("jacket") || l.contains("coat") || l.contains("outer")){prefs.edit().putString("look_outer","None").apply();return "Outer layer removed.";}
            if(l.contains("accessor") || l.contains("necklace") || l.contains("earring") || l.contains("glasses")){prefs.edit().putString("look_accessories","None").apply();return "Accessories removed.";}
            if(l.contains("shoes")){prefs.edit().putString("look_shoes","Barefoot").apply();return "Shoes removed.";}
            if(l.contains("shirt") || l.contains("top")){prefs.edit().putString("look_top","None").apply();return "Top layer removed in the avatar wardrobe state.";}
            if(l.contains("pants") || l.contains("shorts") || l.contains("skirt") || l.contains("bottom")){prefs.edit().putString("look_bottom","None").apply();return "Bottom layer removed in the avatar wardrobe state.";}
            return "Tell me which clothing layer you want removed.";
        }
        if(l.contains("jacket")){prefs.edit().putString("look_outer","Cropped jacket").apply();return "Trying the cropped jacket.";}
        if(l.contains("coat")){prefs.edit().putString("look_outer","Long coat").apply();return "Long coat it is.";}
        if(l.contains("tee") || l.contains("t-shirt")){prefs.edit().putString("look_top","Relaxed tee").apply();return "Changed to a relaxed tee.";}
        if(l.contains("sweater")){prefs.edit().putString("look_top","Soft sweater").apply();return "Soft sweater selected.";}
        if(l.contains("shorts")){prefs.edit().putString("look_bottom","Relaxed shorts").apply();return "Changed to shorts.";}
        if(l.contains("skirt")){prefs.edit().putString("look_bottom","Long skirt").apply();return "Changed to a long skirt.";}
        if(l.contains("jeans") || l.contains("denim")){prefs.edit().putString("look_bottom","Denim").apply();return "Denim selected.";}
        if(l.contains("ponytail")){prefs.edit().putString("look_hair","High ponytail").apply();return "Ponytail it is.";}
        if(l.contains("braid")){prefs.edit().putString("look_hair","Braided").apply();return "Hair changed to a braid.";}
        new Handler().postDelayed(this::showAppearance,200);
        return "Opening Appearance Studio so we can change that precisely.";
    }

    void showIntegrations(){
        base("Integration Center");
        addCard("PHONE FEATURES\n✓ Voice input\n✓ Local memory\n✓ Context profiles\n✓ Private vault shell\n✓ Floating Lumi overlay\n✓ Emergency SMS test path\n✓ Private Mode session shell");
        addCard("EXTERNAL CONNECTIONS\n○ OpenAI / ChatGPT API — needs API credential + backend\n○ Meta models — needs developer API credential\n○ Ray-Ban Meta device bridge — needs Meta wearable SDK/API\n○ Gmail / Calendar — needs OAuth authorization\n○ Smart home — needs Home Assistant/Alexa/device credentials\n\nThese are connection points, not falsely simulated as live services.");
    }

    void showEmergency(){
        base("Emergency");
        String contact=prefs.getString("emergency_number",""); addCard("PRIMARY CONTACT\n"+(contact.isEmpty()?"Not configured":contact)+"\n\nFlow: suspected emergency → check-in → 30-second cancel window → text + current location when available.");
        Button set=btn("Set emergency phone number");content.addView(set);set.setOnClickListener(v->setEmergencyContact());
        Button test=btn("Run 30-second TEST countdown");content.addView(test);test.setOnClickListener(v->startEmergencyCountdown());
        addCard("TEST MODE SAFETY\nThe test does not send a message automatically. It demonstrates the countdown. Actual automatic SMS requires SEND_SMS permission and should only be enabled after you verify the configured contact.");
    }

    void setEmergencyContact(){
        final EditText e=new EditText(this);e.setInputType(InputType.TYPE_CLASS_PHONE);e.setHint("Phone number");
        new AlertDialog.Builder(this).setTitle("Emergency contact").setView(e).setPositiveButton("Save",(d,w)->{prefs.edit().putString("emergency_number",e.getText().toString().trim()).apply();showEmergency();}).setNegativeButton("Cancel",null).show();
    }

    void startEmergencyCountdown(){
        final AlertDialog box=new AlertDialog.Builder(this).setTitle("Emergency test").setMessage("30 seconds until the test would escalate. Tap CANCEL to stop.").setNegativeButton("CANCEL",null).create(); box.show();
        final Handler h=new Handler(); final int[] sec={30};
        Runnable r=new Runnable(){public void run(){
            if(!box.isShowing())return; sec[0]--;
            if(sec[0]<=0){box.dismiss(); new AlertDialog.Builder(MainActivity.this).setTitle("Test complete").setMessage("In live mode this is where Lumi would send the configured text + location.").setPositiveButton("OK",null).show();}
            else{box.setMessage(sec[0]+" seconds until the test would escalate. Tap CANCEL to stop.");h.postDelayed(this,1000);}
        }};
        h.postDelayed(r,1000);
    }

    void requestContextPermissions(){
        if(Build.VERSION.SDK_INT>=23){
            ArrayList<String> p=new ArrayList<>();
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.RECORD_AUDIO);
            if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),REQ_PERMS);else Toast.makeText(this,"Context permissions already granted",Toast.LENGTH_SHORT).show();
        }
    }

    void openVault(){
        String pin=prefs.getString("pin","");
        if(pin.isEmpty()){ setupPin(); return; }
        final EditText e=new EditText(this); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD); e.setHint("Lumi PIN");
        new AlertDialog.Builder(this).setTitle("Unlock Lumi Vault").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Unlock",(d,w)->{ if(e.getText().toString().equals(pin)) showVault(); else Toast.makeText(this,"Incorrect PIN",Toast.LENGTH_SHORT).show(); }).show();
    }

    void setupPin(){
        final EditText e=new EditText(this); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD); e.setHint("Choose Lumi PIN");
        new AlertDialog.Builder(this).setTitle("Create Lumi Vault PIN").setMessage("Separate from your phone unlock. Prototype storage only; production vault will use encrypted file storage.").setView(e)
                .setPositiveButton("Save",(d,w)->{if(e.getText().length()>=4){prefs.edit().putString("pin",e.getText().toString()).apply();showVault();}else Toast.makeText(this,"Use at least 4 digits",Toast.LENGTH_SHORT).show();})
                .setNegativeButton("Cancel",null).show();
    }

    void showVault(){
        base("Lumi Vault");
        addCard("PRIVATE GALLERY PROTOTYPE\nPIN protected and separate from the normal gallery concept. Production target: encrypted storage, 5-minute unlock window, organization by people / places / objects / moments, and indefinite retention for emergency captures.");
    }

    void requestPrivateMode(){
        if(privateSession){ touchPrivateSession(); showHome(); return; }
        if(!prefs.getBoolean("private_opt_in",false)){ showPrivateConsent(); return; }
        authenticatePrivateMode();
    }

    void showPrivateConsent(){
        new AlertDialog.Builder(this)
                .setTitle("Private Mode")
                .setMessage("Private Mode is an adults-only personal context. By continuing, you confirm you are 18 or older and intentionally want Lumi to use a warmer, more playful or flirtatious conversational style. Core consent, safety, authentication and privacy rules remain active. Private conversation is not saved automatically.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("I'm 18+ • Continue",(d,w)->{prefs.edit().putBoolean("private_opt_in",true).apply();authenticatePrivateMode();})
                .show();
    }

    void authenticatePrivateMode(){
        if(Build.VERSION.SDK_INT>=28){
            try{
                android.hardware.biometrics.BiometricPrompt prompt = new android.hardware.biometrics.BiometricPrompt.Builder(this)
                        .setTitle("Unlock Private Mode")
                        .setSubtitle("Verify it's you")
                        .setDescription("Private Mode closes automatically after inactivity.")
                        .setNegativeButton("Use phone unlock",getMainExecutor(),(d,w)->promptDeviceCredential())
                        .build();
                prompt.authenticate(new android.os.CancellationSignal(),getMainExecutor(),new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback(){
                    @Override public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricPrompt.AuthenticationResult result){
                        super.onAuthenticationSucceeded(result); enterPrivateMode();
                    }
                    @Override public void onAuthenticationError(int errorCode, CharSequence errString){
                        super.onAuthenticationError(errorCode,errString);
                    }
                });
                return;
            }catch(Exception ignored){}
        }
        promptDeviceCredential();
    }

    void promptDeviceCredential(){
        KeyguardManager km=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(km==null){ Toast.makeText(this,"Phone unlock is unavailable.",Toast.LENGTH_LONG).show(); return; }
        Intent intent=km.createConfirmDeviceCredentialIntent("Unlock Private Mode","Confirm your phone PIN, pattern or password.");
        if(intent!=null) startActivityForResult(intent,REQ_PRIVATE_DEVICE_CREDENTIAL);
        else Toast.makeText(this,"Set a secure phone lock before using Private Mode.",Toast.LENGTH_LONG).show();
    }

    void enterPrivateMode(){
        privateSession=true;
        touchPrivateSession();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        stopService(new Intent(this,LumiOverlayService.class));
        showHome();
    }

    void exitPrivateMode(){
        privateSession=false;
        privateSessionExpiresAt=0L;
        privateHandler.removeCallbacks(privateTimeout);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    void touchPrivateSession(){
        if(privateSession){
            privateSessionExpiresAt=System.currentTimeMillis()+PRIVATE_SESSION_MS;
            privateHandler.removeCallbacks(privateTimeout);
            privateHandler.postDelayed(privateTimeout,PRIVATE_SESSION_MS);
        }
    }

    void checkPrivateSession(){
        if(privateSession && System.currentTimeMillis()>privateSessionExpiresAt){
            exitPrivateMode();
            Toast.makeText(this,"Private Mode locked after inactivity.",Toast.LENGTH_SHORT).show();
        }
    }

    void showSettings(){
        checkPrivateSession();
        base("Settings");
        content.addView(tv("Context Filter",18,text));
        RadioGroup rg=new RadioGroup(this); String cur=prefs.getString("filter","Balanced");
        for(String s:new String[]{"Strict","Balanced","Relaxed","Custom"}){
            RadioButton r=new RadioButton(this);r.setText(s);r.setTextColor(text);r.setChecked(s.equals(cur));r.setOnClickListener(v->prefs.edit().putString("filter",s).apply());rg.addView(r);
        }
        content.addView(rg);
        addCard("BEHAVIOR\n✓ Important proactive cues only\n✓ Quiet around other people\n✓ Natural conversation\n✓ Learn from corrections\n✓ High-risk actions require confirmation\n✓ Purchases require approval");

        if(privateSession){
            content.addView(tv("Private Tone",18,text));
            RadioGroup prg=new RadioGroup(this); String pt=prefs.getString("private_tone","Playful");
            for(String s:new String[]{"Warm","Playful","Flirty","Intimate"}){
                RadioButton r=new RadioButton(this);r.setText(s);r.setTextColor(text);r.setChecked(s.equals(pt));r.setOnClickListener(v->prefs.edit().putString("private_tone",s).apply());prg.addView(r);
            }
            content.addView(prg);
            addCard("Private Tone changes Lumi's conversational style only. It never disables consent, safety, authentication, or privacy rules.");
        }

        Button change=btn("Change Lumi Vault PIN"); change.setOnClickListener(v->{prefs.edit().remove("pin").apply();setupPin();});content.addView(change);
        Button overlay=btn("Grant floating-overlay permission"); overlay.setOnClickListener(v->requestOverlay()); content.addView(overlay);
    }

    void requestOverlay(){
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));
        } else Toast.makeText(this,"Overlay permission already available",Toast.LENGTH_SHORT).show();
    }

    void showOverlay(){
        if(privateSession){
            Toast.makeText(this,"Floating overlay is disabled during Private Mode.",Toast.LENGTH_LONG).show();
            return;
        }
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            requestOverlay(); Toast.makeText(this,"Grant overlay permission, then try again",Toast.LENGTH_LONG).show(); return;
        }
        startService(new Intent(this,LumiOverlayService.class));
    }
}
