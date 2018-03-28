/**
 *    Copyright 2006-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.DefaultJavaFormatter;
import org.mybatis.generator.api.dom.java.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DubboServicePlugin extends PluginAdapter {

    private static final String TARGET_PACKAGE = "targetPackage";
    private static final String TARGET_PROJECT = "targetProject";

    private static final String TARGET_IMPL_PACKAGE = "targetImplPackage";
    private static final String TARGET_IMPL_PROJECT = "targetImplProject";


    private List<Method> list = new ArrayList<Method>();

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    /**
     * 生成Service接口
     *
     * @param introspectedTable
     * @param methods
     * @return
     */
    private GeneratedJavaFile generateServiceInterface(IntrospectedTable introspectedTable, List<Method> methods) {

        String targetPackage = getProperties().getProperty(TARGET_PACKAGE);
        String targetProject = getProperties().getProperty(TARGET_PROJECT);
//        FullyQualifiedJavaType entityType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
        FullyQualifiedJavaType entityType = introspectedTable.getRules().calculateAllFieldsClass();
        String domainObjectName = introspectedTable.getFullyQualifiedTable().getDomainObjectName();
        // 生成 Service 名称
        String service = targetPackage + "." + domainObjectName + "Service";
        // 构造 Service 文件
        Interface interfaze = new Interface(new FullyQualifiedJavaType(service));

        // 设置作用域
        interfaze.setVisibility(JavaVisibility.PUBLIC);
        if (methods != null && methods.size() > 0) {
            for (Method m : methods) {
                Method method = new Method(m);
                String originalReturnType = m.getReturnType().getShortNameWithoutTypeArguments();
                method.setReturnType(wrapFullyQualifiedJavaType(method, entityType.getShortName()));
                if ("List".equalsIgnoreCase(originalReturnType)) {
                    Method nonePageableMethod = new Method(method);
                    method.addParameter(1, new Parameter(new FullyQualifiedJavaType("com.yk.hornet.common.domain.PageBounds"), "pageBounds"));
//                    method.addParameter(2, new Parameter(FullyQualifiedJavaType.getIntInstance(), "pageSize"));
                    interfaze.addMethod(nonePageableMethod);
                }
                interfaze.addMethod(method);
            }
        }
        // import
        interfaze.addImportedType(entityType);
        interfaze.addImportedType(new FullyQualifiedJavaType("com.yk.hornet.common.domain.DataStore"));
        interfaze.addImportedType(new FullyQualifiedJavaType("com.yk.hornet.common.domain.PageBounds"));
        return new GeneratedJavaFile(interfaze, targetProject, new DefaultJavaFormatter());
    }

    /**
     * 生成Service实现
     *
     * @param introspectedTable
     * @param methods
     * @return
     */
    private GeneratedJavaFile generateServiceInterfaceImpl(IntrospectedTable introspectedTable, List<Method> methods) {
        String targetPackage = getProperties().getProperty(TARGET_PACKAGE);
        String targetImplPackage = getProperties().getProperty(TARGET_IMPL_PACKAGE);
        String targetImplProject = getProperties().getProperty(TARGET_IMPL_PROJECT);

        String mapperPackage = introspectedTable.getContext().getJavaClientGeneratorConfiguration().getTargetPackage();

        FullyQualifiedJavaType entityType = introspectedTable.getRules().calculateAllFieldsClass();

        String domainObjectName = introspectedTable.getFullyQualifiedTable().getDomainObjectName();
        // 生成 Service 名称
        String service = targetPackage + "." + domainObjectName + "Service";
        String serviceImpl = targetImplPackage + "." + introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "ServiceImpl";

        TopLevelClass clazz = new TopLevelClass(new FullyQualifiedJavaType(serviceImpl));

        clazz.addAnnotation("@Service");
//        clazz.addAnnotation("@Component");
        clazz.setVisibility(JavaVisibility.PUBLIC);
        clazz.addSuperInterface(new FullyQualifiedJavaType(service));

        // Filed
        String mapperName = domainObjectName + "Mapper";
        Field field = new Field(firstLetterLowerCase(mapperName), new FullyQualifiedJavaType(mapperPackage + "." + mapperName));
        field.addAnnotation("@Autowired");
        field.setVisibility(JavaVisibility.PRIVATE);

        if (methods != null && methods.size() > 0) {
            for (Method m : methods) {
                String originalReturnType = m.getReturnType().getShortNameWithoutTypeArguments();
                Method method = new Method(m);

                method.setReturnType(wrapFullyQualifiedJavaType(method, entityType.getShortName()));
                StringBuffer buffer = new StringBuffer();

                buffer.append("return DataStore.of(").append(firstLetterLowerCase(mapperName)).append(".").append(method.getName());
                buffer.append("(");
                List<Parameter> parameters = m.getParameters();
                if (parameters != null && parameters.size() > 0) {
                    for (Parameter parameter : parameters) {
                        String parameterName = parameter.getName();
                        FullyQualifiedJavaType type = parameter.getType();
                        clazz.addImportedType(type);
                        buffer.append(parameterName);
                    }
                }
                buffer.append("))").append(";");
                // 生成分页

                if ("List".equalsIgnoreCase(originalReturnType)) {
                    Method nonePageableMethod = new Method(method);
                    StringBuffer nonePageableMethodBody = new StringBuffer(buffer.toString());
                    nonePageableMethod.addBodyLine(nonePageableMethodBody.toString());
                    clazz.addMethod(nonePageableMethod);

                    method.addParameter(1, new Parameter(new FullyQualifiedJavaType("com.yk.hornet.common.domain.PageBounds"), "pageBounds"));
//                    method.addParameter(2, new Parameter(FullyQualifiedJavaType.getIntInstance(), "pageSize"));
                    clazz.addImportedType(new FullyQualifiedJavaType("com.github.pagehelper.PageHelper"));
                    buffer.insert(0, "if(pageBounds != null){if(pageBounds.getOrderBy()!=null&&!pageBounds.getOrderBy().equalsIgnoreCase(\"\")){PageHelper.startPage(pageBounds.getStartIndex(),pageBounds.getPageSize(),pageBounds.getOrderBy());}else{PageHelper.startPage(pageBounds.getStartIndex(),pageBounds.getPageSize());}}");
//                    buffer.insert(0, "PageHelper.startPage(startIndex,pageSize);");
                }

                method.addBodyLine(buffer.toString());
                clazz.addMethod(method);

            }
        }

        clazz.addField(field);
        clazz.addImportedType(entityType);
        clazz.addImportedType(service);
//        clazz.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Component"));
        clazz.addImportedType(new FullyQualifiedJavaType("org.springframework.beans.factory.annotation.Autowired"));
        clazz.addImportedType(new FullyQualifiedJavaType("com.alibaba.dubbo.config.annotation.Service"));
        clazz.addImportedType(new FullyQualifiedJavaType("com.yk.hornet.common.domain.DataStore"));
        clazz.addImportedType(new FullyQualifiedJavaType("com.yk.hornet.common.domain.PageBounds"));
        clazz.addImportedType(new FullyQualifiedJavaType(mapperPackage + "." + mapperName));
        return new GeneratedJavaFile(clazz, targetImplProject, new DefaultJavaFormatter());
    }

    private FullyQualifiedJavaType wrapFullyQualifiedJavaType(Method method, String domainObjectName) {
        FullyQualifiedJavaType returnType = method.getReturnType();
        FullyQualifiedJavaType genericReturnType = null;
        if (returnType.isPrimitive()) {
            genericReturnType = new FullyQualifiedJavaType("DataStore<" + returnType.getPrimitiveTypeWrapper().getShortName() + ">");
        } else {
            genericReturnType = new FullyQualifiedJavaType("DataStore<" + domainObjectName + ">");
        }
        return genericReturnType;
    }


    private String firstLetterLowerCase(String name) {
        char c = name.charAt(0);
        if (c >= 'A' && c <= 'Z') {
            String temp = String.valueOf(c);
            return name.replaceFirst(temp, temp.toLowerCase());
        }
        return name;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        List<Method> methodCopy = new ArrayList<Method>(list);
        list.clear();
        return Arrays.asList(generateServiceInterface(introspectedTable, methodCopy), generateServiceInterfaceImpl(introspectedTable, methodCopy));
    }


    @Override
    public boolean clientBasicCountMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientBasicCountMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientBasicDeleteMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientBasicDeleteMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientBasicInsertMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientBasicInsertMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientBasicSelectManyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientBasicSelectManyMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientBasicSelectOneMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientBasicSelectOneMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientBasicUpdateMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientBasicUpdateMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientCountByExampleMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientCountByExampleMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientDeleteByExampleMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientDeleteByExampleMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientDeleteByPrimaryKeyMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientDeleteByPrimaryKeyMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientInsertMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientInsertMethodGenerated(method, topLevelClass, introspectedTable);
    }


    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientSelectByExampleWithBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientSelectByExampleWithBLOBsMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientSelectByExampleWithoutBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientSelectByExampleWithoutBLOBsMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientSelectByPrimaryKeyMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientSelectByPrimaryKeyMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByExampleSelectiveMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByExampleSelectiveMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByExampleWithBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByExampleWithBLOBsMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByExampleWithoutBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByExampleWithoutBLOBsMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByPrimaryKeySelectiveMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByPrimaryKeySelectiveMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(method, topLevelClass, introspectedTable);
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        list.add(method);
        return super.clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(method, topLevelClass, introspectedTable);
    }


}
