package com.hm.homemaking.mapper;

import com.hm.homemaking.domain.HmServiceCategory;
import java.util.List;

public interface HmCategoryMapper
{
    List<HmServiceCategory> selectCategoryList(HmServiceCategory query);

    HmServiceCategory selectCategoryById(Long categoryId);

    int insertCategory(HmServiceCategory category);

    int updateCategory(HmServiceCategory category);

    int deleteCategoryById(Long categoryId);
}
