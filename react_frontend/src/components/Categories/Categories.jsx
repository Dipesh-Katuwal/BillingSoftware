import { useContext } from "react";
import { AppContext } from "../../Context/AppContext";
import "./Categories.css";

const Categories = () => {
  const { categories, selectedCategoryId, setSelectedCategoryId } =
    useContext(AppContext);

  return (
    <section className="explore-categories">
      <header className="explore-categories-heading">
        <div>
          <h2>Categories</h2>
          <p>Select “All items” to clear the category filter.</p>
        </div>
        <span className="explore-categories-count">
          {categories.length} {categories.length === 1 ? "category" : "categories"}
        </span>
      </header>

      {categories.length > 0 ? (
        <div className="explore-category-list">
          <button
            className={`explore-category-card explore-all-category-card ${
              selectedCategoryId === null ? "selected" : ""
            }`}
            type="button"
            aria-pressed={selectedCategoryId === null}
            onClick={() => setSelectedCategoryId(null)}
          >
            <div className="explore-category-shade" />
            <div className="explore-category-details">
              <h3>All items</h3>
              <span>Show every item</span>
            </div>
          </button>
          {categories.map((category) => (
            <button
              className={`explore-category-card ${
                selectedCategoryId === category.categoryId
                  ? "selected"
                  : ""
              }`}
              key={category.categoryId}
              type="button"
              aria-pressed={selectedCategoryId === category.categoryId}
              onClick={() =>
                setSelectedCategoryId((currentCategoryId) =>
                  currentCategoryId === category.categoryId
                    ? null
                    : category.categoryId,
                )
              }
              style={{ backgroundColor: category.bgColor || "#343a40" }}
            >
              {category.imgUrl && (
                <img
                  className="explore-category-image"
                  src={category.imgUrl}
                  alt=""
                  loading="lazy"
                />
              )}
              <div className="explore-category-shade" />
              <div className="explore-category-details">
                <h3>{category.name}</h3>
                <span>
                  {category.items ?? 0}{" "}
                  {(category.items ?? 0) === 1 ? "item" : "items"}
                </span>
              </div>
            </button>
          ))}
        </div>
      ) : (
        <p className="explore-category-empty">No categories available yet.</p>
      )}
    </section>
  );
};

export default Categories;