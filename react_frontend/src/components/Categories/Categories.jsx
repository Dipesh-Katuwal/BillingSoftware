import { useContext } from "react";
import { AppContext } from "../../Context/AppContext";
import "./Categories.css";

const Categories = () => {
  const { categories } = useContext(AppContext);

  return (
    <section className="explore-categories">
      <header className="explore-categories-heading">
        <div>
          <h2>Categories</h2>
          <p>Browse your products by category</p>
        </div>
        <span className="explore-categories-count">
          {categories.length} {categories.length === 1 ? "category" : "categories"}
        </span>
      </header>

      {categories.length > 0 ? (
        <div className="explore-category-list">
          {categories.map((category) => (
            <article
              className="explore-category-card"
              key={category.categoryId}
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
            </article>
          ))}
        </div>
      ) : (
        <p className="explore-category-empty">No categories available yet.</p>
      )}
    </section>
  );
};

export default Categories;