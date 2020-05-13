package com.buct.museumguide.ui.home;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.buct.museumguide.MainActivity;
import com.buct.museumguide.R;
import com.buct.museumguide.Service.CommandRequest;
import com.buct.museumguide.Service.MediaPlaybackService;
import com.buct.museumguide.Service.ResultMessage;
import com.buct.museumguide.Service.StateBroadCast;
import com.buct.museumguide.Service.StringMessage;
import com.buct.museumguide.Service.loginstatemessage;
import com.buct.museumguide.bean.LoginState;
import com.buct.museumguide.ui.FragmentForMain.CommonList.CommonList;
import com.buct.museumguide.ui.FragmentForUsers.Login.Login;
import com.buct.museumguide.ui.map.MapGuide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.youth.banner.Banner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/*
* 系统的默认页面，直接在这里构建页面0以及跳转逻辑，该页面的显示应按fragment实现
* 计划按逻辑实现为每个主要的方式（如地图/博物馆各种信息为一个具体的信息，其他的为fragment）
* */
public class HomeFragment extends Fragment {

    private static final String TAG = HomeFragment.class.getSimpleName();
    private HomeViewModel homeViewModel;
    private Banner homeBanner;
    private Button playbutton;
    private AlertDialog.Builder builder;
    private SharedPreferences Infos;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Infos = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE);
        String info = Infos.getString("info", "");
        if (info.equals("") == false) {
            Toast.makeText(getActivity(), info, Toast.LENGTH_SHORT).show();
            System.out.println(info);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //开始轮播
        EventBus.getDefault().register(this);

        homeBanner.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        //结束轮播
        homeBanner.stop();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        System.out.println(getActivity());

        final SearchView homeSearch = root.findViewById(R.id.homeSearch);
        homeSearch.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_navigation_home_to_searchResult);
        });
        final CardView cardViewIntro = root.findViewById(R.id.cardViewIntro);
        cardViewIntro.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_navigation_home_to_museumInfo);
        });
        final CardView cardViewComment = root.findViewById(R.id.cardViewComment);
        cardViewComment.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_navigation_home_to_userComment);
        });
        final Button button2 = root.findViewById(R.id.button2);
        button2.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MapGuide.class));
        });
        final Button homeMyComment = root.findViewById(R.id.homeMyComment);
        homeMyComment.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_navigation_home_to_myComment);
        });

        final TextView museumListButton = root.findViewById(R.id.museumList_button);
        museumListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.action_navigation_home_to_museumList);
            }
        });

        homeBanner = root.findViewById(R.id.homeBanner);
        homeBanner.setAdapter(new HomeBannerAdapter(getContext(), MuseumItem.getTestData()))
                .setOnBannerListener((data, position) -> {
                    MuseumItem mData = (MuseumItem) data;
                    Bundle bundle = new Bundle();
                    bundle.putInt("showType", mData.viewType);
                    Navigation.findNavController(getView()).navigate(R.id.action_navigation_home_to_commonList, bundle);
                })
                .start();
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("HomeFragment onResume");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
    }

    @Subscribe
    public void GetResult(ResultMessage msg) {
        System.out.println("homefragment得到" + msg.res);
    }

    @Subscribe
    public void GetState(StateBroadCast msg) {
        if (msg.state == 1) {
            System.out.println("收到了服务已启动的通知");
        } else {
            EventBus.getDefault()
                    .post(new
                            CommandRequest
                            ("http://192.144.239.176:8080/api/android/get_education_activity_info"));
        }
    }
/*
* @ loginstatemessage 返回登录请求，按需决定是否跳转到登录页面
* */
    @Subscribe
    public void GetLoginState(loginstatemessage msg) {
        String res = msg.res;
        Gson gson = new Gson();
        LoginState state = gson.fromJson(res, LoginState.class);
        Boolean islogin = state.getData().getIs_login();
        System.out.println("登录状态" + state.getData().getIs_login());/**/
        if (!islogin) {
            Infos.edit().putString("user", "").apply();//首次启动
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("请先登录");// 设置标题
            builder.setCancelable(false);
            // 为对话框设置取消按钮
            builder.setPositiveButton("去登录", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Navigation.findNavController(getView()).navigate(R.id.action_navigation_home_to_login);
                }
            });
            Looper.prepare();
            builder.create().show();// 使用show()方法显示对话框
            Looper.loop();
        }
    }
}