package com.jetbrains.internal.descriptorsdiagram;

import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author ksafonov
 */
public class DescriptorDiagramNode implements DiagramNode<XmlFile> {
    private final XmlFile myFile;
    private final DiagramProvider<XmlFile> myProvider;

    public DescriptorDiagramNode(XmlFile file, DiagramProvider<XmlFile> provider) {
        myFile = file;
        myProvider = provider;
    }

    @Override
    public String getName() {
        return myFile.getName();
    }

    @Override
    public Icon getIcon() {
        return myFile.getIcon(Iconable.ICON_FLAG_READ_STATUS);
    }

    @NotNull
    @Override
    public XmlFile getIdentifyingElement() {
        return myFile;
    }

    @NotNull
    @Override
    public DiagramProvider<XmlFile> getProvider() {
        return myProvider;
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    }
}
