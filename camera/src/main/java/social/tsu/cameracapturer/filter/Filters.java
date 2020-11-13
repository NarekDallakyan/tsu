package social.tsu.cameracapturer.filter;

import androidx.annotation.NonNull;

import social.tsu.camera.CameraView;
import social.tsu.cameracapturer.filters.AutoFixFilter;
import social.tsu.cameracapturer.filters.BlackAndWhiteFilter;
import social.tsu.cameracapturer.filters.BrightnessFilter;
import social.tsu.cameracapturer.filters.ContrastFilter;
import social.tsu.cameracapturer.filters.CrossProcessFilter;
import social.tsu.cameracapturer.filters.DocumentaryFilter;
import social.tsu.cameracapturer.filters.DuotoneFilter;
import social.tsu.cameracapturer.filters.FillLightFilter;
import social.tsu.cameracapturer.filters.GammaFilter;
import social.tsu.cameracapturer.filters.GrainFilter;
import social.tsu.cameracapturer.filters.GrayscaleFilter;
import social.tsu.cameracapturer.filters.HueFilter;
import social.tsu.cameracapturer.filters.InvertColorsFilter;
import social.tsu.cameracapturer.filters.LomoishFilter;
import social.tsu.cameracapturer.filters.PosterizeFilter;
import social.tsu.cameracapturer.filters.SaturationFilter;
import social.tsu.cameracapturer.filters.SepiaFilter;
import social.tsu.cameracapturer.filters.SharpnessFilter;
import social.tsu.cameracapturer.filters.TemperatureFilter;
import social.tsu.cameracapturer.filters.TintFilter;
import social.tsu.cameracapturer.filters.VignetteFilter;

/**
 * Contains commonly used {@link Filter}s.
 * <p>
 * You can use {@link #newInstance()} to create a new instance and
 * pass it to {@link CameraView#setFilter(Filter)}.
 */
public enum Filters {

    /**
     * @see NoFilter
     */
    NONE(NoFilter.class),

    /**
     * @see AutoFixFilter
     */
    AUTO_FIX(AutoFixFilter.class),

    /**
     * @see BlackAndWhiteFilter
     */
    BLACK_AND_WHITE(BlackAndWhiteFilter.class),

    /**
     * @see BrightnessFilter
     */
    BRIGHTNESS(BrightnessFilter.class),

    /**
     * @see ContrastFilter
     */
    CONTRAST(ContrastFilter.class),

    /**
     * @see CrossProcessFilter
     */
    CROSS_PROCESS(CrossProcessFilter.class),

    /**
     * @see DocumentaryFilter
     */
    DOCUMENTARY(DocumentaryFilter.class),

    /**
     * @see DuotoneFilter
     */
    DUOTONE(DuotoneFilter.class),

    /**
     * @see FillLightFilter
     */
    FILL_LIGHT(FillLightFilter.class),

    /**
     * @see GammaFilter
     */
    GAMMA(GammaFilter.class),

    /**
     * @see GrainFilter
     */
    GRAIN(GrainFilter.class),

    /**
     * @see GrayscaleFilter
     */
    GRAYSCALE(GrayscaleFilter.class),

    /**
     * @see HueFilter
     */
    HUE(HueFilter.class),

    /**
     * @see InvertColorsFilter
     */
    INVERT_COLORS(InvertColorsFilter.class),

    /**
     * @see LomoishFilter
     */
    LOMOISH(LomoishFilter.class),

    /**
     * @see PosterizeFilter
     */
    POSTERIZE(PosterizeFilter.class),

    /**
     * @see SaturationFilter
     */
    SATURATION(SaturationFilter.class),

    /**
     * @see SepiaFilter
     */
    SEPIA(SepiaFilter.class),

    /**
     * @see SharpnessFilter
     */
    SHARPNESS(SharpnessFilter.class),

    /**
     * @see TemperatureFilter
     */
    TEMPERATURE(TemperatureFilter.class),

    /**
     * @see TintFilter
     */
    TINT(TintFilter.class),

    /**
     * @see VignetteFilter
     */
    VIGNETTE(VignetteFilter.class);

    private Class<? extends Filter> filterClass;

    Filters(@NonNull Class<? extends Filter> filterClass) {
        this.filterClass = filterClass;
    }

    /**
     * Returns a new instance of the given filter.
     *
     * @return a new instance
     */
    @NonNull
    public Filter newInstance() {
        try {
            return filterClass.newInstance();
        } catch (IllegalAccessException e) {
            return new NoFilter();
        } catch (InstantiationException e) {
            return new NoFilter();
        }
    }
}
