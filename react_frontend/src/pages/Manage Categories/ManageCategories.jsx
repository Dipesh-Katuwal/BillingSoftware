import React from 'react'
import './ManageCategories.css'
import { CategoryForm } from '../../components/ManageCategories/CategoryForm'
import { CategoryList } from '../../components/ManageCategories/CategoryList'

export const ManageCategories = () => {
  return (
    <div className="category-container text-light">
      <div className="category-form-column">
        <CategoryForm/>
      </div>
      <div className="category-list-column">
        <CategoryList/>
      </div>
    </div>
  )
}
