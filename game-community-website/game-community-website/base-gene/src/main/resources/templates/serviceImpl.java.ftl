package ${packageName};

import com.sys.${author}.pojo.${tableName};
import com.sys.${author}.service.${tableName}Service;
import com.sys.${author}.mapper.${tableName}Mapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * ${conment!} 服务实现类
 * </p>
 *
 * @author
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${tableName}ServiceImpl extends ServiceImpl<${tableName}Mapper, ${tableName}> implements ${tableName}Service {

}
