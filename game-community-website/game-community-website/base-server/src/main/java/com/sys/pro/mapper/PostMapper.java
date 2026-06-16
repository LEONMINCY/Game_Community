package com.sys.pro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.pro.pojo.Post;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sys.pro.dto.PostPageDTO;
import com.sys.pro.vo.PostPageVo;
import com.sys.pro.vo.PostVo;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

/**
 * PostMapper 是社区帖子的数据访问入口，负责MyBatis-Plus基础读写和扩展查询。
 */
public interface PostMapper extends BaseMapper<Post> {



    /**
     * 完成帖子中的 selectPostPage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param page page 字段，来源于当前接口入参或内部调用上下文。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 帖子分页结果，包含当前页数据和总数。
     */

    Page<PostPageVo> selectPostPage(Page<PostPageVo> page, @Param("dto") PostPageDTO dto);



    /**
     * 读取帖子的 FavoritesByUserId 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 帖子列表数据。
     */

    List<PostVo> getFavoritesByUserId(@Param("userId") Integer userId);



}

