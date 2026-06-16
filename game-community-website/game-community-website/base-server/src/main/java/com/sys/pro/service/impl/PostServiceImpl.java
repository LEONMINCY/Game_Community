package com.sys.pro.service.impl;

import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.Post;
import com.sys.pro.service.PostService;
import com.sys.pro.mapper.PostMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.dto.PostPageDTO;
import com.sys.pro.utils.HashUtil;
import com.sys.pro.vo.PostPageVo;
import com.sys.pro.vo.PostVo;

import java.util.List;

/**
 * 帖子基础服务实现，负责帖子表的持久化、审核状态和互动计数维护。
 */
@Slf4j

@Service

@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {



    private final PostMapper postMapper;



    /**
     * 完成帖子中的 pagePost 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 帖子分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<PostPageVo> pagePost(PostPageDTO dto) {
        Page<PostPageVo> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        Page<PostPageVo> pageResult = baseMapper.selectPostPage(page, dto);
        return PageResult.of(pageResult);
    }

    /**
     * 读取帖子的 FavoritesByUserId 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 帖子列表数据。
     */
    @Override
    public List<PostVo> getFavoritesByUserId(Integer userId) {
        // 从帖子表中查询收藏字段包含当前用户ID的帖子
        List<PostVo> favorites = postMapper.getFavoritesByUserId(userId);
        favorites.forEach(post -> post.setUserLevel(HashUtil.calculateLevel(post.getUserLevel() == null ? 0 : post.getUserLevel())));
        return favorites;
    }

}
