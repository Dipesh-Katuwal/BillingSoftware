import { useState, useEffect, useContext } from "react";
import { toast } from "react-toastify";
import { AppContext } from "../../Context/AppContext";
import { ItemsAPI } from "../../api/ItemsAPI";
import { assets } from "../../assets/assets";
import { CURRENCY_CODE, CURRENCY_SYMBOL } from "../../utils/currency.js";

const ItemForm = () => {
  const { categories, setCategories, setItems } = useContext(AppContext);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [price, setPrice] = useState("");
  const [image, setImage] = useState(null);
  const [imagePreview, setImagePreview] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!image) {
      setImagePreview("");
      return;
    }

    const previewUrl = URL.createObjectURL(image);
    setImagePreview(previewUrl);

    return () => URL.revokeObjectURL(previewUrl);
  }, [image]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSaving(true);

    try {
      const createdItem = await ItemsAPI.addItem(
        { name, price: Number(price), description, categoryId },
        image,
      );
      setItems((currentItems) => [createdItem, ...currentItems]);
      setCategories((currentCategories) =>
        currentCategories.map((category) =>
          category.categoryId === createdItem.categoryId
            ? { ...category, items: (category.items ?? 0) + 1 }
            : category,
        ),
      );
      setName("");
      setDescription("");
      setCategoryId("");
      setPrice("");
      setImage(null);
      toast.success(`Item ${name} added successfully.`);
    } catch (submitError) {
      setError(
        submitError.response?.data?.message ||
          submitError.message ||
          "Unable to save the Item.",
      );
      toast.error("Unable to save the item.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form
      className="card col-12 text-dark item-form-card"
      onSubmit={handleSubmit}
    >
      <div className="card-body p-4">
        <div className="item-form-heading">
          <h2>Add an item</h2>
          <p>Enter the item details and choose its category.</p>
        </div>

        <div className="item-image-field mb-4">
          <label
            className="item-image-upload border rounded d-flex align-items-center justify-content-center overflow-hidden"
            htmlFor="item-image"
            title="Choose item image"
          >
            {imagePreview ? (
              <img
                src={imagePreview}
                alt="item preview"
                className="w-100 h-100 object-fit-cover"
              />
            ) : (
              <img src={assets.upload} alt="" className="w-100 h-100" />
            )}
          </label>
          <div>
            <span className="item-image-title">Item image</span>
            <small>Optional · Click to upload</small>
          </div>
          <input
            id="item-image"
            type="file"
            accept="image/*"
            className="visually-hidden"
            onChange={(event) => setImage(event.target.files?.[0] || null)}
          />
        </div>

        <div className="mb-3">
          <label htmlFor="item-name" className="form-label fw-semibold">
            Name
          </label>
          <input
            id="item-name"
            type="text"
            className="form-control"
            placeholder="Item Name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </div>

        <div className="mb-3">
          <label htmlFor="item-description" className="form-label fw-semibold">
            Description
          </label>
          <textarea
            id="item-description"
            className="form-control"
            rows="5"
            placeholder="Write content here..."
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="category">
            Category
          </label>
          <select
            className="form-control"
            name="item-category"
            id="item-category"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            required
          >
            <option value="">-- SELECT CATEGORY --</option>
            {categories.map((category) => (
              <option key={category.categoryId} value={category.categoryId}>
                {category.name}
              </option>
            ))}
          </select>
        </div>
        <div className="mb-3">
          <label htmlFor="price" className="form-label">
            Price ({CURRENCY_CODE})
          </label>
          <div className="input-group">
            <span className="input-group-text">{CURRENCY_SYMBOL}</span>
            <input
              type="number"
              name="price"
              id="price"
              className="form-control"
              placeholder="0.00"
              min="0"
              step="0.01"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              required
            />
          </div>
        </div>
        {error && <div className="alert alert-danger py-2">{error}</div>}
        <button
          type="submit"
          className="btn btn-primary w-100 item-save-button"
          disabled={isSaving}
        >
          {isSaving ? "Saving..." : "Save"}
        </button>
      </div>
    </form>
  );
};
export default ItemForm;
