package com.jetbrains.internal.descriptorsdiagram;

import com.intellij.diagram.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ksafonov
 */
public class DescriptorsDiagramProvider extends DiagramProvider<XmlFile> {


  private DiagramVfsResolver<XmlFile> myVfsResolver = new DescriptorsVfsResolver();
  private DescriptorsElementManager myElementManager = new DescriptorsElementManager();
  private DiagramNodeContentManager myNodeContentManager = new DescriptorsNodeContentManager();
  private DiagramRelationshipManager<XmlFile> myRelationshipManager = new DiagramRelationshipManager<XmlFile>() {
    @Override
    public DiagramRelationshipInfo getDependencyInfo(XmlFile e1, XmlFile e2, DiagramCategory category) {
      return null;
    }

    @Override
    public DiagramCategory[] getContentCategories() {
      return DiagramCategory.EMPTY_ARRAY;
    }
  };

  @Override
  public String getID() {
    return "descriptors";
  }

  @Override
  public DiagramVisibilityManager createVisibilityManager() {
    return EmptyDiagramVisibilityManager.INSTANCE;
  }

  @Override
  public DiagramNodeContentManager getNodeContentManager() {
    return myNodeContentManager;
  }

  @Override
  public DiagramElementManager<XmlFile> getElementManager() {
    return myElementManager;
  }

  @Override
  public DiagramVfsResolver<XmlFile> getVfsResolver() {
    return myVfsResolver;
  }

  @Override
  public DiagramRelationshipManager<XmlFile> getRelationshipManager() {
    return myRelationshipManager;
  }

  @Override
  public String getPresentableName() {
    return "XML descriptors dependencies";
  }

  @Override
  public DiagramDataModel<XmlFile> createDataModel(@NotNull Project project,
                                                   @Nullable XmlFile element,
                                                   @Nullable VirtualFile file,
                                                   DiagramPresentationModel presentationModel) {
    return new DescriptorsDataModel(element, this);
  }
}
