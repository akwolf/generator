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
package org.mybatis.generator.codegen.mybatis3.javamapper.elements;

import org.mybatis.generator.api.dom.java.*;

import java.util.Set;
import java.util.TreeSet;

public class SelectCountByCdtMethodGenerator extends AbstractJavaMapperMethodGenerator {
    private boolean isSimple;

    public SelectCountByCdtMethodGenerator(boolean isSimple)
    {
        super();
        this.isSimple = isSimple;
    }

    @Override
    public void addInterfaceElements(Interface interfaze)
    {
        Set<FullyQualifiedJavaType> importedTypes = new TreeSet<FullyQualifiedJavaType>();
        Method method = new Method();
        method.setVisibility(JavaVisibility.PUBLIC);

        FullyQualifiedJavaType parameterType = introspectedTable.getRules().calculateAllFieldsClass();
        FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getNewListInstance().getNewListInstance();
        returnType.addTypeArgument(parameterType);
        method.setReturnType(FullyQualifiedJavaType.getIntInstance());
        importedTypes.add(returnType);
        importedTypes.add(parameterType);
//        importedTypes.add(FullyQualifiedJavaType.getPageBoundsType());
        //importedTypes.add(FullyQualifiedJavaType.getDataSourceType());

        method.setName(introspectedTable.getSelectCountByCdtStatementId());

        method.addParameter(new Parameter(parameterType, "record")); //$NON-NLS-1$
//        method.addParameter(new Parameter(FullyQualifiedJavaType.getPageBoundsType(), "pageBounds"));
        addMapperAnnotations(interfaze, method);

        context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

        if (context.getPlugins().clientSelectByPrimaryKeyMethodGenerated(method, interfaze, introspectedTable))
        {
            interfaze.addImportedTypes(importedTypes);
            interfaze.addMethod(method);
        }
    }

    public void addMapperAnnotations(Interface interfaze, Method method)
    {
        return;
    }
}
