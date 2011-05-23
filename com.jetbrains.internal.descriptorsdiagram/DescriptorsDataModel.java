package com.jetbrains.internal.descriptorsdiagram;

import com.intellij.diagram.*;
import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author ksafonov
 */
public class DescriptorsDataModel extends DiagramDataModel<XmlFile> {
    private final Map<String, XmlFile> myElementsAddedByUser = new HashMap<String, XmlFile>();
    private final Map<String, XmlFile> myElementsRemovedByUser = new HashMap<String, XmlFile>();

    private final Collection<DiagramNode<XmlFile>> myNodes = new HashSet<DiagramNode<XmlFile>>();
    private final Collection<DiagramEdge<XmlFile>> myEdges = new HashSet<DiagramEdge<XmlFile>>();

    private final Collection<DiagramNode<XmlFile>> myNodesOld = new HashSet<DiagramNode<XmlFile>>();
    private final Collection<DiagramEdge<XmlFile>> myEdgesOld = new HashSet<DiagramEdge<XmlFile>>();

    private Map<XmlFile, CachedValue<Collection<XmlFile>>> myDependencies = new HashMap<XmlFile, CachedValue<Collection<XmlFile>>>();
    private Map<XmlFile, CachedValue<Collection<XmlFile>>> myDependents = new HashMap<XmlFile, CachedValue<Collection<XmlFile>>>();

    public DescriptorsDataModel(final XmlFile file, DiagramProvider<XmlFile> provider) {
        super(file.getProject(), provider);
        addWithDependenciesAndDependents(file);
    }

    private void addWithDependenciesAndDependents(XmlFile file) {
        XmlFile exists = myElementsAddedByUser.put(DescriptorsVfsResolver.getQualifiedNameStatic(file), file);
        if (exists != null) {
            return;
        }

        Collection<XmlFile> processed = new HashSet<XmlFile>();
        Collection<XmlFile> dependencies = findDependencies(file, processed);
        for (XmlFile dependency : dependencies) {
            addWithDependenciesAndDependents(dependency);
        }

        for (XmlFile dependant : findDependents(file)) {
            if (!processed.contains(dependant)) {
                addWithDependenciesAndDependents(dependant);
            }
        }
    }

    private Collection<XmlFile> findDependencies(final XmlFile file, final Collection<XmlFile> processed) {
        CachedValue<Collection<XmlFile>> value = myDependencies.get(file);
        if (value == null) {
            value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new DependenciesProvider(file), false);
            myDependencies.put(file, value);
        }
        Collection<XmlFile> result = new ArrayList<XmlFile>(value.getValue());
        result.removeAll(processed);
        return result;
    }

    private Collection<XmlFile> findDependents(XmlFile file) {
        CachedValue<Collection<XmlFile>> value = myDependents.get(file);
        if (value == null) {
            value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new DependentsProvider(file), false);
            myDependents.put(file, value);
        }
        return value.getValue();
    }

    private static boolean isIncludeTag(XmlTag tag) {
        return "include".equals(tag.getLocalName()) && "http://www.w3.org/2001/XInclude".equals(tag.getNamespace());
    }

    @NotNull
    public Collection<DiagramNode<XmlFile>> getNodes() {
        return myNodes;
    }

    @NotNull
    public Collection<DiagramEdge<XmlFile>> getEdges() {
        return myEdges;
    }

    @Override
    public DiagramNode<XmlFile> addElement(XmlFile element) {
        if (findNode(element) != null) return null;

        final String fqn = DescriptorsVfsResolver.getQualifiedNameStatic(element);
        myElementsAddedByUser.put(fqn, element);
        myElementsRemovedByUser.remove(fqn);

        setupScopeManager(element, true);

        return new DescriptorDiagramNode(element, getProvider());
    }

    @NotNull
    @NonNls
    public String getNodeName(final DiagramNode<XmlFile> node) {
        return node.getIdentifyingElement().getName();
    }

    @Override
    public void removeNode(DiagramNode<XmlFile> node) {
        removeElement(node.getIdentifyingElement());
    }

    public void refreshDataModel() {
        clearAll();
        updateDataModel();
    }

    @NotNull
    @Override
    public ModificationTracker getModificationTracker() {
        return PsiManager.getInstance(getProject()).getModificationTracker();
    }

    private void clearAll() {
        clearAndBackup(myNodes, myNodesOld);
        clearAndBackup(myEdges, myEdgesOld);
    }

    private boolean isAllowedToShow(XmlFile aFile) {
        if (aFile == null || !aFile.isValid()) return false;
        for (XmlFile file : myElementsRemovedByUser.values()) {
            if (file.equals(aFile)) return false;
        }

        final DiagramScopeManager<XmlFile> scopeManager = getScopeManager();
        return !(scopeManager != null && !scopeManager.contains(aFile));
    }

    public synchronized void updateDataModel() {
        final Set<XmlFile> files = getAllFiles();

        for (XmlFile file : files) {
            if (isAllowedToShow(file)) {
                myNodes.add(new DescriptorDiagramNode(file, getProvider()));
            }
        }

        for (XmlFile file : files) {
            DiagramNode<XmlFile> source = findNode(file);
            Collection<XmlFile> processed = new HashSet<XmlFile>();
            Collection<XmlFile> dependencies = findDependencies(file, processed);
            for (XmlFile dependency : dependencies) {
                DiagramNode<XmlFile> target = findNode(dependency);
                if (target != null) {
                    addEdge(source, target);
                }
            }
        }
        mergeWithBackup(myNodes, myNodesOld);
        mergeWithBackup(myEdges, myEdgesOld);
    }

    private DescriptorsUmlEdge addEdge(DiagramNode<XmlFile> source, DiagramNode<XmlFile> target) {
        for (DiagramEdge edge : myEdges) {
            if (edge.getSource() == source && edge.getTarget() == target) return null;
        }
        DescriptorsUmlEdge result = new DescriptorsUmlEdge(source, target);
        myEdges.add(result);
        return result;
    }

    private static <T> void clearAndBackup(Collection<T> target, Collection<T> backup) {
        backup.clear();
        backup.addAll(target);
        target.clear();
    }

    private static <T> void mergeWithBackup(Collection<T> target, Collection<T> backup) {
        for (T t : backup) {
            if (target.contains(t)) {
                target.remove(t);
                target.add(t);
            }
        }
    }

    //public JSUmlEdge addEdge(DiagramNode<XmlFile> from, DiagramNode<XmlFile> to, DiagramRelationshipInfo relationship) {
    //  return addEdge(from, to, relationship, myEdges);
    //}

    //private static JSUmlEdge addEdge(DiagramNode<XmlFile> from,
    //                                 DiagramNode<XmlFile> to,
    //                                 DiagramRelationshipInfo relationship,
    //                                 Collection<DiagramEdge<XmlFile>> storage) {
    //  for (DiagramEdge edge : storage) {
    //    if (edge.getSource() == from && edge.getTarget() == to && edge.getRelationship() == relationship) return null;
    //  }
    //  JSUmlEdge result = new JSUmlEdge(from, to, relationship);
    //  storage.add(result);
    //  return result;
    //}

    private Set<XmlFile> getAllFiles() {
        Set<XmlFile> result = new HashSet<XmlFile>(myElementsAddedByUser.values());
        result.removeAll(myElementsRemovedByUser.values());
        return result;
    }

    @Nullable
    public DiagramNode<XmlFile> findNode(XmlFile object) {
        for (DiagramNode<XmlFile> node : myNodes) {
            final String fqn = DescriptorsVfsResolver.getQualifiedNameStatic(node.getIdentifyingElement());
            if (fqn != null && fqn.equals(DescriptorsVfsResolver.getQualifiedNameStatic(object))) {
                return node;
            }
        }
        return null;
    }

    public void dispose() {
    }

    public void removeElement(XmlFile element) {
        DiagramNode node = findNode(element);
        String qName = DescriptorsVfsResolver.getQualifiedNameStatic(element);
        if (node == null) {
            myElementsAddedByUser.remove(qName);
            return;
        }

        Collection<DiagramEdge> edges = new ArrayList<DiagramEdge>();
        for (DiagramEdge edge : myEdges) {
            if (node.equals(edge.getTarget()) || node.equals(edge.getSource())) {
                edges.add(edge);
            }
        }
        myEdges.removeAll(edges);
        myNodes.remove(node);
        myElementsRemovedByUser.put(qName, element);
        myElementsAddedByUser.remove(qName);
    }

    @Override
    public boolean isPsiListener() {
        return true;
    }

    @Nullable
    public static Object getIdentifyingElement(DiagramNode node) {
        if (node instanceof DescriptorDiagramNode) {
            return node.getIdentifyingElement();
        }
        if (node instanceof DiagramNoteNode) {
            final DiagramNode delegate = ((DiagramNoteNode) node).getIdentifyingElement();
            if (delegate != node) {
                return getIdentifyingElement(delegate);
            }
        }
        return null;
    }

    private static class DependenciesProvider implements CachedValueProvider<Collection<XmlFile>> {
        private final XmlFile file;

        public DependenciesProvider(XmlFile file) {
            this.file = file;
        }

        @Override
        public Result<Collection<XmlFile>> compute() {
            final Collection<XmlFile> result = new HashSet<XmlFile>();
            file.accept(new XmlRecursiveElementVisitor() {
                @Override
                public void visitXmlTag(XmlTag tag) {
                    if (isIncludeTag(tag)) {
                        String relativePath = tag.getAttributeValue("href");
                        int index = relativePath.lastIndexOf("/");
                        String name = index != -1 ? relativePath.substring(index + 1) : relativePath;
                        PsiFile[] files =
                                FilenameIndex.getFilesByName(file.getProject(), name, GlobalSearchScope.projectScope(file.getProject()));
                        for (PsiFile file : files) {
                            if (file instanceof XmlFile) {
                                result.add((XmlFile) file);
                            }
                        }
                    }
                    super.visitXmlTag(tag);
                }
            });
            return new Result<Collection<XmlFile>>(result, file);
        }
    }

    private class DependentsProvider implements CachedValueProvider<Collection<XmlFile>> {
        private final XmlFile file;

        public DependentsProvider(XmlFile file) {
            this.file = file;
        }

        @Override
        public Result<Collection<XmlFile>> compute() {
            Project project = file.getProject();
            VirtualFile baseDir = project.getBaseDir();
            FindModel model = new FindModel();
            model.setCaseSensitive(false);
            model.setStringToFind(file.getName());
            model.setWholeWordsOnly(true);
            model.setFindAll(true);
            model.setWithSubdirectories(true);
            model.setMultipleFiles(true);
            model.setFileFilter("*.xml");
            List<UsageInfo> usages = FindInProjectUtil.findUsages(model, PsiManager.getInstance(project).findDirectory(baseDir), project);

            List<XmlFile> result = ContainerUtil.mapNotNull(usages, new NullableFunction<UsageInfo, XmlFile>() {
                @Override
                public XmlFile fun(UsageInfo usageInfo) {
                    PsiElement element = usageInfo.getElement();
                    if (element == null) {
                        return null;
                    }
                    TextRange range = usageInfo.getRangeInElement();
                    if (range == null) {
                        return null;
                    }
                    PsiElement elementAt = element.findElementAt(range.getStartOffset());
                    if (elementAt == null) {
                        return null;
                    }
                    XmlTag tag = PsiTreeUtil.getParentOfType(elementAt, XmlTag.class);
                    if (tag == null || !isIncludeTag(tag)) {
                        return null;
                    }
                    return (XmlFile) element.getContainingFile();
                }
            });
            return new Result<Collection<XmlFile>>(result, result.toArray(new Object[result.size()]));
        }
    }
}
