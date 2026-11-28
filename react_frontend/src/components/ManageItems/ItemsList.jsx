import { useContext, useEffect, useState } from "react";
import { toast } from "react-toastify";
import { MdDelete } from "react-icons/md";
import { AppContext } from "../../Context/AppContext";
import { ItemsAPI } from "../../api/ItemsAPI";
import { formatNpr } from "../../utils/currency.js";

const ItemsList = () => {
  const { items, setCategories, setItems } = useContext(AppContext);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");

  const visibleItems = items.filter((item) =>
    item.name.toLowerCase().includes(searchTerm.trim().toLowerCase()),
  );

  useEffect(() => {
    const loadItems = async () => {
      try {
        const allItems = await ItemsAPI.getItems();
        setItems(allItems);
      } catch (error) {
        console.error("Unable to load items:", error);
        toast.error("Unable to load items.");
      } finally {
        setIsLoading(false);
      }
    };

    loadItems();
  }, [setItems]);

  const deleteItem = async (item) => {
    try {
      await ItemsAPI.deleteItem(item.itemId);
      setItems((currentItems) =>
        currentItems.filter((currentItem) => currentItem.itemId !== item.itemId),
      );
      setCategories((currentCategories) =>
        currentCategories.map((category) =>
          category.categoryId === item.categoryId
            ? { ...category, items: Math.max(0, (category.items ?? 0) - 1) }
            : category,
        ),
      );
      toast.success(`"${item.name}" deleted successfully.`);
    } catch (error) {
      console.error("Unable to delete item:", error);
      toast.error(`Could not delete "${item.name}". Please try again.`);
    }
  };

  return (
    <div className="items-list-container">
      <h2 className="items-list-title">Items</h2>
      <div className="items-search">
        <input
          className="form-control"
          type="search"
          placeholder="Search items"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />
      </div>
      <div className="item-cards">
        {isLoading ? (
          <p className="items-empty-state">Loading items...</p>
        ) : (
          <>
            {visibleItems.map((item) => (
              <div className="item-card" key={item.itemId}>
                <div className="item-card-content">
                  <div className="item-image-frame">
                    {!item.imgUrl && (
                      <span className="item-image-placeholder" aria-hidden="true">
                        IMG
                      </span>
                    )}
                    {item.imgUrl && (
                      <img
                        src={item.imgUrl}
                        alt={item.name}
                        className="item-image"
                      />
                    )}
                  </div>
                  <div className="item-card-details">
                    <h2>{item.name}</h2>
                    <p>{item.categoryName} · {formatNpr(item.price)}</p>
                    {item.description && <p>{item.description}</p>}
                  </div>
                  <button
                    className="btn btn-danger item-delete-button"
                    type="button"
                    aria-label={`Delete ${item.name}`}
                    onClick={() => deleteItem(item)}
                  >
                    <MdDelete />
                  </button>
                </div>
              </div>
            ))}
            {visibleItems.length === 0 && (
              <p className="items-empty-state">
                {items.length === 0
                  ? "No items yet."
                  : "No matching items."}
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default ItemsList;
