package com.jetbrains.internal.descriptorsdiagram;

import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;

/**
 * @author ksafonov
 */
public class DescriptorsVfsResolver implements DiagramVfsResolver<XmlFile> {
  @Override
  public String getQualifiedName(XmlFile element) {
    return getQualifiedNameStatic(element);
  }

  static String getQualifiedNameStatic(XmlFile element) {
    return VfsUtil.getRelativePath(element.getVirtualFile(), element.getProject().getBaseDir(), '/').replace("/", "\\");
  }

  @Override
  public XmlFile resolveElementByFQN(String fqn, Project project) {
    VirtualFile vFile = VfsUtil.findRelativeFile(fqn, project.getBaseDir());
    PsiFile file = vFile != null ? PsiManager.getInstance(project).findFile(vFile) : null;
    return file instanceof XmlFile ? (XmlFile)file : null;
  }
}
