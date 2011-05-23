package com.jetbrains.internal.descriptorsdiagram;

import com.intellij.diagram.AbstractDiagramNodeContentManager;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;
import com.intellij.openapi.module.Module;
import com.intellij.util.Icons;

public class DescriptorsNodeContentManager extends AbstractDiagramNodeContentManager {

  private static final DiagramCategory MODULE = new DiagramCategory("Show modules", Icons.MODULES_SOURCE_FOLDERS_ICON);

  private final static DiagramCategory[] CATEGORIES = {MODULE};

  public DiagramCategory[] getContentCategories() {
    return CATEGORIES;
  }

  public boolean isInCategory(Object obj, DiagramCategory category, DiagramState presentation) {
    return category == MODULE && obj instanceof Module;
  }
}
