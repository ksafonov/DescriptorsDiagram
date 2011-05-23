package com.jetbrains.internal.descriptorsdiagram;

import com.intellij.diagram.AbstractDiagramElementManager;
import com.intellij.diagram.presentation.DiagramState;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.SimpleColoredText;

import javax.swing.*;

/**
 * @author ksafonov
 */
public class DescriptorsElementManager extends AbstractDiagramElementManager<XmlFile> {
  @Override
  public XmlFile findInDataContext(DataContext context) {
    Project project = PlatformDataKeys.PROJECT.getData(context);
    if (project == null) {
      return null;
    }

    PsiFile[] files = FilenameIndex.getFilesByName(project, "PlatformLangPlugin.xml", GlobalSearchScope.projectScope(project));
    if (files.length == 1 && files[0] instanceof XmlFile) {
      return (XmlFile)files[0];
    }
    else {
      return null;
    }
  }

  @Override
  public boolean isAcceptableAsNode(Object element) {
    return element instanceof XmlFile;
  }

  @Override
  public String getElementTitle(XmlFile element) {
    return element.getName();
  }

  private static String getName(XmlFile element) {
    return element.getName();
  }

  @Override
  public SimpleColoredText getPresentableName(Object element, DiagramState presentation) {
    if (element instanceof XmlFile) {
      return new SimpleColoredText(getName((XmlFile)element), DEFAULT_TITLE_ATTR);
    }
    else if (element instanceof Module) {
      return new SimpleColoredText(((Module)element).getName(), DEFAULT_TEXT_ATTR);
    }
    return null;
  }

  @Override
  public String getElementDescription(XmlFile element) {
    return null;
  }

  @Override
  public Object[] getNodeElements(XmlFile parent) {
    Module module = ModuleUtil.findModuleForPsiElement(parent);
    return module != null ? new Object[]{module} : EMPTY_ARRAY;
  }

  @Override
  public Icon getNodeElementIcon(Object element, DiagramState presentation) {
    return element instanceof Module ? ((Module)element).getModuleType().getNodeIcon(false) : null;
  }
}
