package com.sys.pro.service;

import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.dto.PostPageDTO;
import com.sys.pro.vo.PostPageVo;
import com.sys.pro.vo.PostVo;

import java.util.List;

/**
 * PostService 定义社区帖子业务能力，供控制器和其他服务组合调用。
 */
public interface PostService extends IService<Post> {



    /**
     * 完成帖子中的 pagePost 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 帖子分页结果，包含当前页数据和总数。
     */

    PageResult<PostPageVo> pagePost(PostPageDTO dto);



    /**
     * 读取帖子的 FavoritesByUserId 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 帖子列表数据。
     */

    List<PostVo> getFavoritesByUserId(Integer userId);



}

