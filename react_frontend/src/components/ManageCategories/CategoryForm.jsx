import { useEffect, useState } from "react";
import { categoriesAPI } from "../../api/categoriesAPI";

export const CategoryForm = () => {
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
      await categoriesAPI.addCategory(
        { name, description, backgroundColor },
        image,
      );
      setName("");
      setDescription("");
      setBackgroundColor("#000000");
      setImage(null);
    } catch (submitError) {
      setError(
        submitError.response?.data?.message ||
          submitError.message ||
          "Unable to save the category.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form
      className="card col-8 h-100 align-self-start text-dark"
      onSubmit={handleSubmit}
    >
      <div className="card-body p-4">
        <div className="mb-4">
          <label
            className="border rounded bg-light d-flex align-items-center justify-content-center overflow-hidden"
            htmlFor="category-image"
            style={{ width: "48px", height: "48px", cursor: "pointer" }}
            title="Choose category image"
          >
            {imagePreview ? (
              <img
                src={imagePreview}
                alt="Category preview"
                className="w-100 h-100 object-fit-cover"
              />
            ) : (
              <span className="text-secondary small">48 x 48</span>
            )}
          </label>
          <input
            id="category-image"
            type="file"
            accept="image/*"
            className="visually-hidden"
            onChange={(event) => setImage(event.target.files?.[0] || null)}
          />
        </div>

        <div className="mb-3">
          <label htmlFor="category-name" className="form-label fw-semibold">
            Name
          </label>
          <input
            id="category-name"
            type="text"
            className="form-control"
            placeholder="Category Name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </div>

        <div className="mb-3">
          <label
            htmlFor="category-description"
            className="form-label fw-semibold"
          >
            Description
          </label>
          <textarea
            id="category-description"
            className="form-control"
            rows="5"
            placeholder="Write content here..."
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </div>

        <div className="mb-3">
          <label
            htmlFor="category-background-color"
            className="form-label fw-semibold"
          >
            Background color
          </label>
          <input
            id="category-background-color"
            type="color"
            className="form-control form-control-color d-block"
            value={backgroundColor}
            onChange={(event) => setBackgroundColor(event.target.value)}
            title="Choose category background color"
          />
          <input
            id="category-background-color"
            type="color"
            className="form-control form-control-color d-block"
            value="B64848FF"
            onChange={(event) => setBackgroundColor(event.target.value)}
            title="Choose category background color"
          />
        </div>

        <button type="submit" className="btn btn-primary w-100">
          {isSaving ? "Saving..." : "Save"}
        </button>
      </div>
    </form>
  );
};
