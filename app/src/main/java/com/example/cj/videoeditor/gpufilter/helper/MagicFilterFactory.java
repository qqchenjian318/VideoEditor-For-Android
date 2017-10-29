package com.example.cj.videoeditor.gpufilter.helper;


import com.example.cj.videoeditor.gpufilter.basefilter.GPUImageFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicAntiqueFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicBrannanFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicCoolFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicFreudFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicHefeFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicHudsonFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicInkwellFilter;
import com.example.cj.videoeditor.gpufilter.filter.MagicN1977Filter;
import com.example.cj.videoeditor.gpufilter.filter.MagicNashvilleFilter;

public class MagicFilterFactory {

    private static MagicFilterType filterType = MagicFilterType.NONE;

    public static GPUImageFilter initFilters(MagicFilterType type) {
        if (type == null) {
            return null;
        }
        filterType = type;
        switch (type) {
            case ANTIQUE:
                return new MagicAntiqueFilter();
            case BRANNAN:
                return new MagicBrannanFilter();
            case FREUD:
                return new MagicFreudFilter();
            case HEFE:
                return new MagicHefeFilter();
            case HUDSON:
                return new MagicHudsonFilter();
            case INKWELL:
                return new MagicInkwellFilter();
            case N1977:
                return new MagicN1977Filter();
            case NASHVILLE:
                return new MagicNashvilleFilter();
            case COOL:
                return new MagicCoolFilter();
            case WARM:
                return new MagicWarmFilter();
            default:
                return null;
        }
    }

    public MagicFilterType getCurrentFilterType() {
        return filterType;
    }

    private static class MagicWarmFilter extends GPUImageFilter {
    }
}
