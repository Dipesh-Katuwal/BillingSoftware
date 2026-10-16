import React from 'react'
import './ManageCategories.css'
import { CategoryForm } from '../../components/ManageCategories/CategoryForm'
import { CategoryList } from '../../components/ManageCategories/CategoryList'
import { AppContext } from '../../Context/AppContext'

export const ManageCategories = () => {
  const {categories} = useState(AppContext);
  
  return (
    <div className="category-container text-light">
      <div className="left-column">
        <CategoryForm/>
      </div>
      <div className="right-column">
        <CategoryList categories={categories}/>
      </div>
    </div>
  )
}
