package social.tsu.camerarecorder.widget;

import android.content.Context;
import android.graphics.BitmapFactory;

import java.io.InputStream;

import social.tsu.camerarecorder.egl.filter.GlBilateralFilter;
import social.tsu.camerarecorder.egl.filter.GlBoxBlurFilter;
import social.tsu.camerarecorder.egl.filter.GlBulgeDistortionFilter;
import social.tsu.camerarecorder.egl.filter.GlCGAColorspaceFilter;
import social.tsu.camerarecorder.egl.filter.GlFilter;
import social.tsu.camerarecorder.egl.filter.GlFilterGroup;
import social.tsu.camerarecorder.egl.filter.GlGaussianBlurFilter;
import social.tsu.camerarecorder.egl.filter.GlGrayScaleFilter;
import social.tsu.camerarecorder.egl.filter.GlInvertFilter;
import social.tsu.camerarecorder.egl.filter.GlLookUpTableFilter;
import social.tsu.camerarecorder.egl.filter.GlMonochromeFilter;
import social.tsu.camerarecorder.egl.filter.GlSepiaFilter;
import social.tsu.camerarecorder.egl.filter.GlSharpenFilter;
import social.tsu.camerarecorder.egl.filter.GlSphereRefractionFilter;
import social.tsu.camerarecorder.egl.filter.GlToneCurveFilter;
import social.tsu.camerarecorder.egl.filter.GlToneFilter;
import social.tsu.camerarecorder.egl.filter.GlVignetteFilter;
import social.tsu.camerarecorder.egl.filter.GlWeakPixelInclusionFilter;
import social.tsu.filter.R;

public enum Filters {
    NORMAL,
    BILATERAL,
    BOX_BLUR,
    BULGE_DISTORTION,
    CGA_COLOR_SPACE,
    GAUSSIAN_BLUR,
    GLAY_SCALE,
    INVERT,
    LOOKUP_TABLE,
    MONOCHROME,
    OVERLAY,
    SEPIA,
    SHARPEN,
    SPHERE_REFRACTION,
    TONE_CURVE,
    TONE,
    VIGNETTE,
    WEAKPIXELINCLUSION,
    FILTER_GROUP;

    public static GlFilter getFilterInstance(Filters filters, Context applicationContext) {
        switch (filters) {
            case BILATERAL:
                return new GlBilateralFilter();
            case BOX_BLUR:
                return new GlBoxBlurFilter();
            case BULGE_DISTORTION:
                return new GlBulgeDistortionFilter();
            case CGA_COLOR_SPACE:
                return new GlCGAColorspaceFilter();
            case GAUSSIAN_BLUR:
                return new GlGaussianBlurFilter();
            case GLAY_SCALE:
                return new GlGrayScaleFilter();
            case INVERT:
                return new GlInvertFilter();
            case LOOKUP_TABLE:
                return new GlLookUpTableFilter(BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.lookup_sample));
            case MONOCHROME:
                return new GlMonochromeFilter();
            case SEPIA:
                return new GlSepiaFilter();
            case SHARPEN:
                return new GlSharpenFilter();
            case SPHERE_REFRACTION:
                return new GlSphereRefractionFilter();
            case TONE_CURVE:
                try {
                    InputStream inputStream = applicationContext.getAssets().open("acv/tone_cuver_sample.acv");
                    return new GlToneCurveFilter(inputStream);
                } catch (Exception e) {
                    return new GlFilter();
                }
            case TONE:
                return new GlToneFilter();
            case VIGNETTE:
                return new GlVignetteFilter();
            case WEAKPIXELINCLUSION:
                return new GlWeakPixelInclusionFilter();
            case FILTER_GROUP:
                return new GlFilterGroup(new GlMonochromeFilter(), new GlVignetteFilter());

            default:
                return new GlFilter();
        }
    }
}
