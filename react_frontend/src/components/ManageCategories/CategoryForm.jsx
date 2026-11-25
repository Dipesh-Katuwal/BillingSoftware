import { useContext, useEffect, useState } from "react";
import { categoriesAPI } from "../../api/categoriesAPI";
import { assets } from "../../assets/assets";
import { toast } from "react-toastify";
import { AppContext } from "../../Context/AppContext";

export const CategoryForm = () => {
  const { setCategories } = useContext(AppContext);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [backgroundColor, setBackgroundColor] = useState("#2C3335");
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
      const createdCategory = await categoriesAPI.addCategory(
        { name, description, bgColor: backgroundColor },
        image,
      );
      setCategories((currentCategories) => [
        createdCategory,
        ...currentCategories,
      ]);
      setName("");
      setDescription("");
      setBackgroundColor("#2c2c2c");
      setImage(null);
      toast.success(`category ${name} added successfully`);
    } catch (submitError) {
      setError(
        submitError.response?.data?.message ||
          submitError.message ||
          "Unable to save the category.",
      );
      toast.error("Unable to save the category.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form
      className="card col-12 align-self-start text-dark category-form-card"
      onSubmit={handleSubmit}
    >
      <div className="card-body p-4">
        <div className="category-form-heading">
          <h2>Create a category</h2>
          <p>Add a name and details to organize your products.</p>
        </div>

        <div className="category-image-field mb-4">
          <label
            className="category-image-upload border rounded d-flex align-items-center justify-content-center overflow-hidden"
            htmlFor="category-image"
            title="Choose category image"
          >
            {imagePreview ? (
              <img
                src={imagePreview}
                alt="Category preview"
                className="w-100 h-100 object-fit-cover"
              />
            ) : (
              <img src={assets.upload} className="w-100 h-100 object-fit-content"/>
            )}
          </label>
          <div>
            <span className="category-image-title">Category image</span>
            <small>Optional · Click to upload</small>
          </div>
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
            rows="4"
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
        </div>

        <button type="submit" className="btn btn-primary w-100 category-save-button">
          {isSaving ? "Saving..." : "Save"}
        </button>
      </div>
    </form>
  );
};
