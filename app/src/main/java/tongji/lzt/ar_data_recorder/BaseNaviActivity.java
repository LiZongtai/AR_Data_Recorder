package tongji.lzt.ar_data_recorder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.amap.api.navi.AMapNaviView;

public class BaseNaviActivity extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_base_navi);
        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);

        mAMapNaviView.setAMapNaviViewListener(this);
    }
}