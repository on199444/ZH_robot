package zh_wxassistant.com;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import zh_wxassistant.com.activity.AutoSendMsgActivity;
import zh_wxassistant.com.communication.AssistantToWx;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        SimpleAdapter listitemAdapter = new SimpleAdapter();
        ((ListView) findViewById(R.id.listview_main)).setAdapter(listitemAdapter);
    }

    // Me1nu 列表
    String[] items = {"指定人名语音发送消息", "收到微信信息", "语音发送朋友圈", "朋友圈新动态处理",
            "更多功能敬请期待"};

    @Override
    public void onClick(View view) {
        int tag = Integer.parseInt(view.getTag().toString());
        Intent intent = null;
        switch (tag) {
            case 0:
                //用户主动要求发送消息
                intent = new Intent(MainActivity.this, AutoSendMsgActivity.class);
                break;
            case 1:
                // 用户接收到微信消息
                //intent = new Intent(MainActivity.this, AsrDemo.class);
                break;
            case 2:
                // 用户要求发送朋友圈
                //intent = new Intent(MainActivity.this, UnderstanderDemo.class);
                break;
            case 3:
                // 接受到朋友圈消息
               // intent = new Intent(MainActivity.this, TtsDemo.class);
                break;
            case 4:
                // 更多功能敬请期待
                show("更多");
                break;
            default:
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    private class SimpleAdapter extends BaseAdapter {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                View mView = factory.inflate(R.layout.list_items, null);
                convertView = mView;
            }

            Button btn = (Button) convertView.findViewById(R.id.btn);
            btn.setOnClickListener(MainActivity.this);
            btn.setTag(position);
            btn.setText(items[position]);

            return convertView;
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }

    private void requestPermissions(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void show(String info){
        Toast.makeText(this,info, Toast.LENGTH_SHORT).show();
    }
}
