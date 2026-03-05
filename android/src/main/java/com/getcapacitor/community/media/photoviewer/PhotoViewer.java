package com.getcapacitor.community.media.photoviewer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.media.photoviewer.adapter.Image;
import com.getcapacitor.community.media.photoviewer.fragments.GalleryFullscreenFragment;
import com.getcapacitor.community.media.photoviewer.fragments.ImageFragment;
import com.getcapacitor.community.media.photoviewer.fragments.MainFragment;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

public class PhotoViewer extends BridgeActivity {
    private static final String TAG = "CapacitorPhotoViewer";
    private Context context;
    private int frameLayoutViewId = 8256;
    private Bridge bridge;

    PhotoViewer(Context context, Bridge bridge) {
        this.context = context;
        this.bridge = bridge;
    }

    public String echo(String value) {
        return value;
    }

    public void show(JSArray images, String mode, Integer startFrom, JSObject options) throws Exception {
        try {
            ArrayList<Image> imageList = convertJSArrayToImageList(images);
            Integer stFrom = startFrom > imageList.size() - 1 ? imageList.size() - 1 : startFrom;
            if (imageList.size() > 1 && mode.equals("gallery")) {
                // create the main fragment
                createMainFragment(imageList, options);
            } else if (mode.equals("one")) {
                createImageFragment(imageList, stFrom, options);
            } else if (mode.equals("slider")) {
                createSliderFragment(imageList, stFrom, options);
            }
            return;
        } catch (JSONException e) {
            throw new Exception(e.getMessage());
        }
    }

    private FrameLayout prepareContainer() throws Exception {
        try {
            ViewGroup root = (ViewGroup) bridge.getWebView().getParent();
            if (root == null) {
                throw new Exception("WebView parent is null");
            }

            // Cleanup previously leaked containers (same fixed ID), otherwise fragment may attach
            // to an older container that ended up below WebView after camera preview actions.
            for (int i = root.getChildCount() - 1; i >= 0; i--) {
                View child = root.getChildAt(i);
                if (child != null && child.getId() == frameLayoutViewId) {
                    root.removeViewAt(i);
                }
            }

            FrameLayout frameLayoutView = new FrameLayout(context);
            frameLayoutView.setId(frameLayoutViewId);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            );
            frameLayoutView.setLayoutParams(lp);
            root.addView(frameLayoutView);
            frameLayoutView.bringToFront();
            return frameLayoutView;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private void createMainFragment(ArrayList<Image> imageList, JSObject options) throws Exception {
        try {
            prepareContainer();
            final MainFragment mainFragment = new MainFragment();
            mainFragment.setImageList(imageList);
            mainFragment.setOptions(options);

            bridge
                .getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(frameLayoutViewId, mainFragment, "mainfragment")
                .commit();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private void createImageFragment(ArrayList<Image> imageList, Integer startFrom, JSObject options) throws Exception {
        try {
            FrameLayout frameLayoutView = prepareContainer();

            // account for edge-to-edge top statusbar, buttons should not overlap the statusbar
            ViewCompat.setOnApplyWindowInsetsListener(frameLayoutView, (v, windowInsets) -> {
              Insets sb = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
              v.setPadding(
                v.getPaddingLeft(),
                sb.top,
                v.getPaddingRight(),
                v.getPaddingBottom()
              );
              return windowInsets;  // pass through if children need it too
            });

            final ImageFragment imageFragment = new ImageFragment();
            imageFragment.setImage(imageList.get(startFrom));
            imageFragment.setOptions(options);
            imageFragment.setStartFrom(startFrom);

            bridge
                .getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(frameLayoutViewId, imageFragment, "imagefragment")
                .commit();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private void createSliderFragment(ArrayList<Image> imageList, Integer startFrom, JSObject options) throws Exception {
        try {
            prepareContainer();
            final GalleryFullscreenFragment galleryFragment = new GalleryFullscreenFragment();
            galleryFragment.setImageList(imageList);
            galleryFragment.setStartFrom(startFrom);
            galleryFragment.setMode("slider");
            galleryFragment.setOptions(options);
            galleryFragment.setStartFrom(startFrom);

            bridge
                .getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(frameLayoutViewId, galleryFragment, "gallery")
                .commit();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private ArrayList<Image> convertJSArrayToImageList(JSArray jsArray) throws JSONException {
        ArrayList<Image> list = new ArrayList<Image>();
        for (int i = 0; i < jsArray.length(); i++) {
            if (jsArray.isNull(i)) {
                list.add(null);
            } else {
                Object obj = jsArray.get(i);
                String url = ((JSONObject) obj).getString("url");
                String title = ((JSONObject) obj).optString("title", null);
                list.add(new Image(url, title));
            }
        }
        return list;
    }
}
