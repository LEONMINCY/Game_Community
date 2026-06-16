<el-dialog :visible.sync="dialogVisible" title="${title}">
    <el-form :model="${model}" label-width="120px">
        <#list fields as field>
            <el-form-item label="${field.label}" prop="${field.name}">
                <#if field.type == "input">
                    <el-input v-model="${model}.${field.name}" placeholder="${field.placeholder}"></el-input>
                <#elseif field.type == "number">
                    <el-input v-model.number="${model}.${field.name}" placeholder="${field.placeholder}" type="number"></el-input>
                <#elseif field.type == "date">
                    <el-date-picker v-model="${model}.${field.name}" type="date" placeholder="${field.placeholder}" value-format="yyyy-MM-dd"></el-date-picker>
                <#elseif field.type == "datetime">
                    <el-date-picker v-model="${model}.${field.name}" type="datetime" placeholder="${field.placeholder}" value-format="yyyy-MM-dd HH:mm:ss"></el-date-picker>
                <#elseif field.type == "switch">
                    <el-switch v-model="${model}.${field.name}"></el-switch>
                <#elseif field.type == "select">
                    <api-select v-model="${model}.${field.name}" :url="'${field.optionsApi}'" placeholder="${field.placeholder}" />
                <#else>
                    <el-input v-model="${model}.${field.name}" placeholder="${field.placeholder}"></el-input>
                </#if>
            </el-form-item>
        </#list>
    </el-form>
    <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
    </div>
</el-dialog>
