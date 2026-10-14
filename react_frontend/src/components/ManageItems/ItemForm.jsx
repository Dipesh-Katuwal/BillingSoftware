import {useState,useEffect} from "react";

const ItemForm = () => {

    const [name, setName] = useState('')
    const [description, setDescription] = useState('')
    const [backgroundColor, setBackgroundColor] = useState('#b64848')
    const [image, setImage] = useState(null)
    const [imagePreview, setImagePreview] = useState('')
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!image) {
            setImagePreview('')
            return
        }

        const previewUrl = URL.createObjectURL(image)
        setImagePreview(previewUrl)

        return () => URL.revokeObjectURL(previewUrl)
    }, [image])

    const handleSubmit = async (event) => {
        event.preventDefault()
        setError('')
        setIsSaving(true)

        try {

            setName('')
            setBackgroundColor('#000000')
            setImage(null)
        } catch (submitError) {
            setError(
                submitError.response?.data?.message ||
                submitError.message ||
                'Unable to save the Item.',
            )
        } finally {
            setIsSaving(false)
        }
    }

    return (
        <form className="card col-8 h-100 align-self-start text-dark " onSubmit={handleSubmit}>
            <div className="card-body p-4">
                <div className="mb-4">
                    <label
                        className="border rounded bg-light d-flex align-items-center justify-content-center overflow-hidden"
                        htmlFor="item-image"
                        style={{ width: '48px', height: '48px', cursor: 'pointer' }}
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
                <div className="mb-3" >
                    <label  className="form-label" htmlFor="category">Category</label>
                    <select className="form-control" name="item-category" id="item-category" >
                        <option value="">--SELECT CATEGORY--</option>
                        <option value="category 1">category1</option>
                    </select>
                </div>
                <div className="mb-3">
                    <label htmlFor="item-background-color" className="form-label fw-semibold">
                        Background color
                    </label>
                    <input
                        id="item-background-color"
                        type="color"
                        className="form-control form-control-color d-block"
                        value={backgroundColor}
                        onChange={(event) => setBackgroundColor(event.target.value)}
                        title="Choose item background color"
                    />
                    <input
                        id="item-background-color"
                        type="color"
                        className="form-control form-control-color d-block"
                        value='B64848FF'
                        onChange={(event) => setBackgroundColor(event.target.value)}
                        title="Choose category background color"
                    />
                </div>

                <button type="submit" className="btn btn-primary w-100">
                    {isSaving ? 'Saving...' : 'Save'}
                </button>
            </div>
        </form>
    )
}
export default ItemForm