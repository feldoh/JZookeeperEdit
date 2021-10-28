package net.imagini.jzookeeperedit.fx.view;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import net.imagini.jzookeeperedit.ZkNode;
import net.imagini.jzookeeperedit.ZkTreeNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.control.decoration.Decorator;
import org.controlsfx.control.decoration.StyleClassDecoration;

public class ZkTreeNodeCellFactory implements Callback<TreeView<ZkNode>, TreeCell<ZkNode>> {
    private static final Logger LOGGER = LogManager.getLogger(ZkTreeNodeCellFactory.class);
    private static final Decoration FILTERED_DECORATION = new StyleClassDecoration("tree-cell-filtered");

    @Override
    public TreeCell<ZkNode> call(TreeView<ZkNode> param) {
        return new ZkTreeNodeCell();
    }

    private static class ZkTreeNodeCell extends TreeCell<ZkNode> {
        @Override
        protected void updateItem(ZkNode item, boolean empty) {
            super.updateItem(item, empty);

            if (!empty) {
                Object treeItem = this.getTreeItem();
                if (treeItem instanceof ZkTreeNode) {
                    if (((ZkTreeNode) treeItem).isFiltered()) {
                        Decorator.addDecoration(this, FILTERED_DECORATION);
                    } else {
                        Decorator.removeDecoration(this, FILTERED_DECORATION);
                    }
                }
                setTextIfChanged(item.toString());
            } else {
                this.setText(null);
                this.setGraphic(null);
            }
        }

        private void setTextIfChanged(String newText) {
            if (hasStringChanged(this.getText(), newText)) {
                this.setText(newText);
            }
        }

        private boolean hasStringChanged(String oldString, String newString) {
            LOGGER.debug("Rendered TreeCell: {} -> {}", String.valueOf(oldString),
                    String.valueOf(newString));
            return oldString == null ? newString != null : !oldString.equals(newString);
        }
    }

}
