import { useContext, useEffect, useState } from "react";
import { ItemsAPI } from "../../api/ItemsAPI";
import { AppContext } from "../../Context/AppContext";
import "./Items.css";

const Items = () => {
  const {
    items,
    setItems,
    categories,
    selectedCategoryId,
  } = useContext(AppContext);
  const [searchTerm, setSearchTerm] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadItems = async () => {
      try {
        const allItems = await ItemsAPI.getItems();
        setItems(allItems);
      } catch (loadError) {
        console.error("Unable to load items:", loadError);
        setError("Unable to load items. Please try again.");
      } finally {
        setIsLoading(false);
      }
    };

    loadItems();
  }, [setItems]);

  const visibleItems = items.filter((item) => {
    const matchesCategory =
      !selectedCategoryId || item.categoryId === selectedCategoryId;
    const matchesSearch = `${item.name} ${item.categoryName}`
      .toLowerCase()
      .includes(searchTerm.trim().toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const selectedCategory = categories.find(
    (category) => category.categoryId === selectedCategoryId,
  );

  return (
    <section className="explore-items">
      <header className="explore-items-heading">
        <div className="explore-items-title">
          <h2>Items</h2>
          <span className="explore-items-count">
            {visibleItems.length} {visibleItems.length === 1 ? "item" : "items"}
          </span>
          <p>
            {selectedCategory
              ? `Showing ${selectedCategory.name}`
              : "Browse available products"}
          </p>
        </div>
        <div className="explore-items-search">
          <input
            className="form-control"
            type="search"
            placeholder="Search items"
            aria-label="Search items"
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
          />
        </div>
      </header>

      {isLoading ? (
        <p className="explore-items-message">Loading items...</p>
      ) : error ? (
        <p className="explore-items-message explore-items-error" role="alert">
          {error}
        </p>
      ) : visibleItems.length > 0 ? (
        <div className="explore-item-grid">
          {visibleItems.map((item) => {
            const category = categories.find(
              (currentCategory) =>
                currentCategory.categoryId === item.categoryId,
            );

            return (
              <article className="explore-item-card" key={item.itemId}>
                <div
                  className="explore-item-image"
                  style={{
                    backgroundColor: category?.bgColor || "#343a40",
                  }}
                >
                  {item.imgUrl ? (
                    <img src={item.imgUrl} alt={item.name} loading="lazy" />
                  ) : (
                    <span aria-hidden="true">
                      {item.name.charAt(0).toUpperCase()}
                    </span>
                  )}
                  {item.categoryName && (
                    <span className="explore-item-category">
                      {item.categoryName}
                    </span>
                  )}
                </div>
                <div className="explore-item-details">
                  <div className="explore-item-title-row">
                    <h3>{item.name}</h3>
                    <span className="explore-item-price">
                      ${Number(item.price).toFixed(2)}
                    </span>
                  </div>
                  {item.description && <p>{item.description}</p>}
                </div>
              </article>
            );
          })}
        </div>
      ) : (
        <p className="explore-items-message">
          {items.length === 0
            ? "No items available yet."
            : selectedCategoryId && !searchTerm.trim()
              ? "No items in this category."
              : "No matching items."}
        </p>
      )}
    </section>
  );
};

export default Items;