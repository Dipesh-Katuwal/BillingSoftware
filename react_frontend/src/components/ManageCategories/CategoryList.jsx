import { useContext, useState } from "react";
import { AppContext } from "../../Context/AppContext";
import { MdDelete } from "react-icons/md";
import { categoriesAPI } from "../../api/categoriesAPI";
import { toast } from "react-toastify";

export const CategoryList = () => {
  const { categories, setCategories } = useContext(AppContext);
  const [searchTerm, setSearchTerm] = useState("");

  const visibleCategories = categories.filter((category) =>
    category.name.toLowerCase().includes(searchTerm.trim().toLowerCase()),
  );

  const deleteCategory = async (category) => {
    const index = categories.findIndex(
      (item) => item.categoryId === category.categoryId,
    );

    setCategories((current) =>
      current.filter((item) => item.categoryId !== category.categoryId),
    );

    try {
      await categoriesAPI.deleteCategory(category.categoryId);
      toast.success(`"${category.name}" deleted successfully.`);
    } catch (error) {
      setCategories((current) => {
        if (current.some((item) => item.categoryId === category.categoryId)) {
          return current;
        }
        const restored = [...current];
        restored.splice(Math.min(index, restored.length), 0, category);
        return restored;
      });

      console.error("Category deletion failed:", error);
      toast.error(`Could not delete "${category.name}". Please try again.`);
    }
  };

  return (
    <div className="category-list-container">
      <div className="category-search">
        <input
          className="form-control"
          type="search"
          placeholder="Search categories"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />
      </div>
      <div className="category-cards">
        {visibleCategories.map((category) => (
          <div key={category.categoryId}>
            <div
              className="category-card"
              style={{ backgroundColor: category.bgColor }}
            >
              <div className="category-card-content">
                <div className="category-image-frame">
                  <span
                    className="category-image-placeholder"
                    aria-hidden="true"
                  >
                    IMG
                  </span>
                  <img
                    src={category.imgUrl}
                    alt={category.name}
                    className="category-image"
                  />
                </div>
                <div className="category-card-details">
                  <h2>{category.name}</h2>
                  {category.description && <p>{category.description}</p>}
                </div>
                <button
                  className="btn btn-danger category-delete-button"
                  type="button"
                  onClick={() => {
                    deleteCategory(category);
                  }}
                >
                  <span aria-hidden="true">
                    <MdDelete />
                  </span>
                </button>
              </div>
            </div>
          </div>
        ))}
        {visibleCategories.length === 0 && (
          <p className="category-empty-state">
            {categories.length === 0
              ? "No categories yet."
              : "No matching categories."}
          </p>
        )}
      </div>
    </div>
  );
};
