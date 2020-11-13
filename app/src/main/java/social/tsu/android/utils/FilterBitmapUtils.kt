package social.tsu.android.utils

import android.graphics.PointF
import jp.co.cyberagent.android.gpuimage.filter.*
import social.tsu.cameracapturer.filter.BaseFilter
import social.tsu.cameracapturer.filter.NoFilter
import social.tsu.cameracapturer.filters.*

object FilterBitmapUtils {

    fun getCorrespondingFilter(filter: BaseFilter): GPUImageFilter {

        when (filter) {

            is NoFilter -> {
                return GPUImageFilter()
            }

            is BlackAndWhiteFilter -> {
                return GPUImageWhiteBalanceFilter(
                    5000.0f,
                    0.0f
                )
            }

            is ContrastFilter -> {
                return GPUImageContrastFilter(2.0f)
            }

            is FillLightFilter -> {
                return GPUImageHighlightShadowFilter(
                    0.0f,
                    1.0f
                )
            }

            is GammaFilter -> {
                return GPUImageGammaFilter(2.0f)
            }

            is GrayscaleFilter -> {
                return GPUImageGrayscaleFilter()
            }

            is HueFilter -> {
                return GPUImageHueFilter(90.0f)
            }

            is InvertColorsFilter -> {
                return GPUImageColorInvertFilter()
            }

            is PosterizeFilter -> {
                return GPUImagePosterizeFilter()
            }

            is SaturationFilter -> {
                return GPUImageSaturationFilter(1.0f)
            }

            is SepiaFilter -> {
                return GPUImageSepiaToneFilter()
            }

            is SharpnessFilter -> {
                return GPUImageSharpenFilter()
            }

            is VignetteFilter -> {
                return GPUImageVignetteFilter(
                    PointF(0.5f, 0.5f),
                    floatArrayOf(0.0f, 0.0f, 0.0f),
                    0.3f,
                    0.75f
                )
            }
            else -> {
                return GPUImageFilter()
            }
        }
    }
}