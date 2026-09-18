package com.norbor.myvpn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.wireguard.android.backend.*;
import com.wireguard.config.Config;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int VPN_PERMISSION=1001;
    private TextView statusText,timeText;
    private EditText configEdit;
    private Button connectButton;
    private GoBackend backend;
    private VpnTunnel tunnel;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long connectedAt=0;

    private final Runnable clock=new Runnable(){ public void run(){
        if(connectedAt>0){
            long s=(System.currentTimeMillis()-connectedAt)/1000;
            timeText.setText(String.format("Connection time: %02d:%02d:%02d",s/3600,(s%3600)/60,s%60));
            handler.postDelayed(this,1000);
        }
    }};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_main);
        statusText=findViewById(R.id.statusText); timeText=findViewById(R.id.timeText);
        configEdit=findViewById(R.id.configEdit); connectButton=findViewById(R.id.connectButton);
        Button save=findViewById(R.id.saveButton);
        SharedPreferences p=getSharedPreferences("vpn",MODE_PRIVATE);
        configEdit.setText(p.getString("config",""));
        backend=new GoBackend(getApplicationContext());
        tunnel=new VpnTunnel("MyVPN", state -> runOnUiThread(() -> {
            statusText.setText("Status: "+state.name());
            boolean up=state==Tunnel.State.UP;
            connectButton.setText(up?"DISCONNECT":"CONNECT");
            if(up){ connectedAt=System.currentTimeMillis(); handler.removeCallbacks(clock); handler.post(clock); }
            else { connectedAt=0; timeText.setText("Connection time: 00:00:00"); handler.removeCallbacks(clock); }
        }));
        save.setOnClickListener(v->{p.edit().putString("config",configEdit.getText().toString()).apply();
            Toast.makeText(this,"Configuration saved",Toast.LENGTH_SHORT).show();});
        connectButton.setOnClickListener(v->{ if(tunnel.state==Tunnel.State.UP) disconnect(); else connect();});
    }

    private void connect(){
        String s=configEdit.getText().toString().trim();
        if(s.isEmpty()){ toast("Paste WireGuard config first."); return; }
        try{ Config.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e){toast("Invalid config: "+e.getMessage()); return;}
        Intent i=GoBackend.VpnService.prepare(this);
        if(i!=null) startActivityForResult(i,VPN_PERMISSION); else startTunnel(s);
    }

    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);
        if(r==VPN_PERMISSION){ if(c==Activity.RESULT_OK) startTunnel(configEdit.getText().toString().trim()); else toast("VPN permission denied."); }
    }

    private void startTunnel(String s){ statusText.setText("Status: CONNECTING...");
        executor.execute(()->{try{
            Config cfg=Config.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
            backend.setState(tunnel,Tunnel.State.UP,cfg);
        }catch(Exception e){runOnUiThread(()->toast("VPN error: "+e.getMessage()));}});
    }

    private void disconnect(){ statusText.setText("Status: DISCONNECTING...");
        executor.execute(()->{try{backend.setState(tunnel,Tunnel.State.DOWN,null);}
        catch(Exception e){runOnUiThread(()->toast("Disconnect failed: "+e.getMessage()));}});
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);executor.shutdownNow();super.onDestroy();}

    private static class VpnTunnel implements Tunnel{
        final String name; final StateListener listener; volatile Tunnel.State state=Tunnel.State.DOWN;
        VpnTunnel(String n,StateListener l){name=n;listener=l;}
        public String getName(){return name;}
        public void onStateChange(Tunnel.State s){state=s;listener.onStateChanged(s);}
    }
    interface StateListener{void onStateChanged(Tunnel.State state);}
}
