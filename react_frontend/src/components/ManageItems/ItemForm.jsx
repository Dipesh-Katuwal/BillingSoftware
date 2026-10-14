import { useState, useEffect } from "react";

const ItemForm = () => {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [backgroundColor, setBackgroundColor] = useState("#b64848");
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
      setName("");
      setBackgroundColor("#000000");
      setImage(null);
    } catch (submitError) {
      setError(
        submitError.response?.data?.message ||
          submitError.message ||
          "Unable to save the Item.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form
      className="card col-8 h-100 align-self-start text-dark "
      onSubmit={handleSubmit}
    >
      <div className="card-body p-4">
        <div className="mb-4">
          <label
            className="border rounded bg-light d-flex align-items-center justify-content-center overflow-hidden"
            htmlFor="item-image"
            style={{ width: "48px", height: "48px", cursor: "pointer" }}
            title="Choose item image"
          >
            {imagePreview ? (
              <img
                src={imagePreview}
                alt="item preview"
                className="w-100 h-100 object-fit-cover"
              />
            ) : (
              <span className="text-secondary small">48 x 48</span>
            )}
          </label>
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
            Item Name
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
          >
            <option value="">--SELECT CATEGORY--</option>
            <option value="category 1">category1</option>
          </select>
        </div>
        <div className="mb-3">
          <label htmlFor="price" className="form-label">
            Price
          </label>
          <div className="input-group">
            <span className="input-group-text">$</span>
            <input
              type="number"
              name="price"
              id="price"
              className="form-control"
              placeholder="0.00"
              step=""
            />
          </div>
        </div>
      </div>
      <div className="p-1">
            <button type="submit" className="btn btn-warning w-100">
          {isSaving ? "Saving..." : "Save"}
        </button>
        </div>
    </form>
  );
};
export default ItemForm;
