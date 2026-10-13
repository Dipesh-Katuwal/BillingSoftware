import './App.css'
import MenuBar from './components/MenuBar'
import { Routes,Route } from 'react-router-dom'
import { Dashboard } from './pages/Dashboard/Dashboard'
import { Explore } from './pages/Explore/Explore'
import { ManageCategories } from './pages/Manage Categories/ManageCategories'
import { ManageItems } from './pages/Manage Items/ManageItems'
import { ManageUsers } from './pages/Manage Users/ManageUsers'

function App() {
  return (
    <div>
      <MenuBar/>
      <Routes>
        <Route path='/' element={<Dashboard/>}/>
        <Route path='/dashboard' element={<Dashboard/>}/>
        <Route path='/explore' element={<Explore/>}/>
        <Route path='/categories' element={<ManageCategories/>}/>
        <Route path='/items' element={<ManageItems/>}/>
        <Route path='/users' element={<ManageUsers/>}/>
      </Routes>
    </div>
  )
}

export default App
