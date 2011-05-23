package com.jetbrains.internal.descriptorsdiagram;

import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramRelationshipInfoAdapter;
import com.intellij.psi.xml.XmlFile;

import java.awt.*;

/**
 * @author ksafonov
 */
public class DescriptorsUmlEdge extends DiagramEdgeBase<XmlFile> {

  private static final DiagramRelationshipInfoAdapter RELATIONSHIP = new DiagramRelationshipInfoAdapter("DEPENDENCY") {
    @Override
    public Shape getStartArrow() {
      return DELTA;
    }
  };

  public DescriptorsUmlEdge(DiagramNode<XmlFile> source, DiagramNode<XmlFile> target) {
    super(source, target, RELATIONSHIP);
  }
}
