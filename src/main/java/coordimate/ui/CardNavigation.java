package coordimate.ui;

import javafx.scene.control.ListView;

/** Moves the selection of a card list one card at a time. */
final class CardNavigation {

    private CardNavigation() {}

    /**
     * Selects and reveals the next card in {@code listView}. If no card is selected, selects the first card.
     * At the end of the list, keeps the last card selected. Does nothing if the list is empty.
     */
    static void selectNext(ListView<?> listView) {
        int itemCount = listView.getItems().size();
        if (itemCount == 0) {
            return;
        }

        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        int targetIndex = selectedIndex < 0 ? 0 : Math.min(selectedIndex + 1, itemCount - 1);
        selectAndReveal(listView, targetIndex);
    }

    /**
     * Selects and reveals the previous card in {@code listView}. If no card is selected, selects the last card.
     * At the start of the list, keeps the first card selected. Does nothing if the list is empty.
     */
    static void selectPrevious(ListView<?> listView) {
        int itemCount = listView.getItems().size();
        if (itemCount == 0) {
            return;
        }

        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        int targetIndex = selectedIndex < 0 ? itemCount - 1 : Math.max(selectedIndex - 1, 0);
        selectAndReveal(listView, targetIndex);
    }

    /** Selects the card at {@code targetIndex} and scrolls it into view. */
    static void selectAndReveal(ListView<?> listView, int targetIndex) {
        listView.getSelectionModel().select(targetIndex);
        listView.scrollTo(targetIndex);
    }
}
