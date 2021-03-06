package com.klimaatmobiel.ui.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klimaatmobiel.PusherApplication
import com.klimaatmobiel.domain.*
import com.klimaatmobiel.domain.DTOs.RemoveOrAddedOrderItemDTO
import com.klimaatmobiel.domain.enums.KlimaatMobielApiStatus
import com.klimaatmobiel.domain.enums.SortStatus
import com.klimaatmobiel.ui.adapters.ProductListAdapter
import kotlinx.coroutines.*
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException


class WebshopViewModel(group: Group, private val repository: KlimaatmobielRepository) : ViewModel() {


    private var _status = MutableLiveData<KlimaatMobielApiStatus>()
    val status: LiveData<KlimaatMobielApiStatus> get() = _status

    var posToRefreshInOrderPreviewListItem: Int = -1

    private var _group = MutableLiveData<Group>()
    val group: LiveData<Group> get() = _group

    private var _project = MutableLiveData<Project>()

    val project: LiveData<Project> get() = _project

    private var _filteredList = MutableLiveData<List<Product>>()
    val filteredList: LiveData<List<Product>> get() = _filteredList
    private var filterString = ""
    private var filterCategoryName = ""
    private var sortStatus = SortStatus.Categorie

    private val _navigateToProductDetail = MutableLiveData<List<Long>>()
    val navigateToProductDetail: LiveData<List<Long>> get() = _navigateToProductDetail

    private val _totaleKlimaatScore = MutableLiveData<Int>()
    val totaleKlimaatScore: LiveData<Int> get() = _totaleKlimaatScore

    private val _deleteClicked = MutableLiveData<Boolean>()
    val deleteClicked: LiveData<Boolean> get() = _deleteClicked

    private val _aantalItemsInOrder = MutableLiveData<Int>()
    val aantalItemsInOrder: LiveData<Int> get() = _aantalItemsInOrder

    var customErrorMessage = ""

    private val _addedProduct = MutableLiveData<Boolean>()
    val addedProduct: LiveData<Boolean> get() = _addedProduct


    init {
        _group.value = group // de groep met het project en de order is hier beschikbaar
        _filteredList.value = group.project.products
        _totaleKlimaatScore.value = group.order.avgScore.toInt()

        setAantal()
        loadProject(group.projectId)
    }

    private fun updateKlimaatScore(){
        val total = group.value?.order!!.orderItems.fold(0){sum, element -> sum + element.amount}
        _totaleKlimaatScore.value = group.value?.order!!
            .orderItems
            .fold(0){sum, element -> sum + (element.amount* element.product!!.score)}/total
    }

    fun onProductDetailNavigated() {
        _navigateToProductDetail.value = null
    }

    private fun loadProject(projectId: Long) {
        viewModelScope.launch {
            _project.value = repository.getProject(projectId)
        }
    }

    private fun setAantal(){
        _aantalItemsInOrder.value = getAantalItemsOrder()
    }




    fun getAantalItemsOrder(): Int{
        return _group.value?.order!!.orderItems.fold(0){sum, element -> sum + element.amount}

    }



    fun addProductToOrder(product: Product){
        viewModelScope.launch {


            val addProductToOrderDeferred = repository.addProductToOrder(OrderItem(0,1,null,product.productId, 0),_group.value!!.order.orderId)
            try {
                _status.value = KlimaatMobielApiStatus.LOADING
                val orderItemRes = addProductToOrderDeferred.await()

                if(orderItemRes.removedOrAddedOrderItem.amount > 1){ // the orderitem is already in the orderitem

                    _group.value!!.findOrderItemById(orderItemRes.removedOrAddedOrderItem.orderItemId)!!
                        .amount = orderItemRes.removedOrAddedOrderItem.amount

                    posToRefreshInOrderPreviewListItem = _group.value!!.order.orderItems
                        .indexOf(_group.value!!.findOrderItemById(orderItemRes.removedOrAddedOrderItem.orderItemId))

                } else {
                    posToRefreshInOrderPreviewListItem = -1
                    _group.value!!.order.orderItems.add(orderItemRes.removedOrAddedOrderItem)
                }

                _group.value!!.order.totalOrderPrice = orderItemRes.totalOrderPrice

                _group.value = _group.value // trigger live data change, moet wss niet?

                updateKlimaatScore()

                _addedProduct.value = true;

                _status.value = KlimaatMobielApiStatus.DONE
                setAantal()
                _addedProduct.value = false;
            }catch (e: HttpException) {
                if (e.code() == 404)
                    customErrorMessage = "Kon het product niet toevoegen"
                else
                    customErrorMessage = "Kon het product niet toevoegen"
                _status.value = KlimaatMobielApiStatus.ERROR

            }catch (e: ConnectException) {
                customErrorMessage = "Er is geen internet! probeer later opnieuw"
                _status.value = KlimaatMobielApiStatus.ERROR

            } catch (e: Exception) {
                customErrorMessage = e.message!!
                _status.value = KlimaatMobielApiStatus.ERROR
            }
        }
    }

    fun filterListString(adapter: ProductListAdapter, c: CharSequence) {
        filterString = c.toString().toLowerCase()
        filterList(adapter)
    }

    fun filterListCategoryName(adapter: ProductListAdapter, s: String) {
        filterCategoryName = s
        filterList(adapter)
    }

    private fun filterList(adapter: ProductListAdapter) {
        //Eerst filteren op string
        var result = _group.value!!.project.products.filter { product ->
            product.productName.toLowerCase().contains(filterString)
        }

        //Dan filteren op categorie
        if (filterCategoryName.isNotEmpty()) {
            result = result.filter { product ->
                product.category!!.categoryName == filterCategoryName
            }
        }

        //Daarna sorteren
        when (sortStatus) {
            SortStatus.Categorie -> {
                result = result.sortedBy { p -> p.category!!.categoryName }
            }
            SortStatus.Naam -> {
                result = result.sortedBy { p -> p.productName }
            }
            SortStatus.Prijs -> {
                result = result.sortedBy { p -> p.price }
            }
        }

        _filteredList.value = result

        //Nieuwe lijst laten tonen via adapter
        if (sortStatus == SortStatus.Categorie) {

            adapter.addHeaderAndSubmitList(filteredList.value)
        } else {
            adapter.submitListNoHeaders(filteredList.value)
        }
    }

    fun changeOrderItemAmount(oi: OrderItem, add: Boolean){
            if(oi.amount == 1 && !add) {
                removeOrderItem(oi)
            } else {
                updateOrderItem(oi, add)
            }

    }

    private fun updateOrderItem(oi: OrderItem, add:Boolean){

        viewModelScope.launch {

            var updateOrderItemDeferred: Deferred<RemoveOrAddedOrderItemDTO>
            if(add){
                 updateOrderItemDeferred = repository.addOrderItemByOne(oi, oi.orderItemId)
            }
            else {
                updateOrderItemDeferred = repository.substractOrderItemByOne(oi, oi.orderItemId)
            }

            try {
                _status.value = KlimaatMobielApiStatus.LOADING
                val orderItemRes = updateOrderItemDeferred.await()

                _group.value!!.findOrderItemById(orderItemRes.removedOrAddedOrderItem.orderItemId)!!
                    .amount = orderItemRes.removedOrAddedOrderItem.amount
                _group.value!!.order.totalOrderPrice = orderItemRes.totalOrderPrice
                _group.value!!.order.submitted = false

                posToRefreshInOrderPreviewListItem = _group.value!!.order.orderItems
                    .indexOf(_group.value!!.findOrderItemById(orderItemRes.removedOrAddedOrderItem.orderItemId))



                _group.value = _group.value // trigger live data change, moet wss niet?

                updateKlimaatScore()
                setAantal()
                _status.value = KlimaatMobielApiStatus.DONE

            }catch (e: HttpException) {
                if (e.code() == 404)
                    customErrorMessage = "Kon het product niet aanpassen"
                else
                    customErrorMessage = "Kon het product niet aanpassen"
                _status.value = KlimaatMobielApiStatus.ERROR

            }catch (e: ConnectException) {
                customErrorMessage = "Er is geen internet! probeer later opnieuw"
                _status.value = KlimaatMobielApiStatus.ERROR

            } catch (e: Exception) {
                customErrorMessage = e.message!!
                _status.value = KlimaatMobielApiStatus.ERROR
            }
        }
    }

    fun removeOrderItem(oi : OrderItem){

        viewModelScope.launch {
            val removeOrderItemDeferred = repository.removeOrderItemFromOrder(oi.orderItemId, group.value!!.order.orderId)
            try {
                _status.value = KlimaatMobielApiStatus.LOADING
                val orderItemRes = removeOrderItemDeferred.await()

                _group.value!!.order.orderItems.remove( _group.value!!.findOrderItemById(orderItemRes.removedOrAddedOrderItem.orderItemId)!!)
                _group.value!!.order.totalOrderPrice = orderItemRes.totalOrderPrice
                _group.value!!.order.submitted = false

                posToRefreshInOrderPreviewListItem = -1

                _group.value = _group.value // trigger live data change, moet wss niet?

                updateKlimaatScore()
                setAantal()
                _status.value = KlimaatMobielApiStatus.DONE

            }catch (e: HttpException) {
                if (e.code() == 404)
                    customErrorMessage = "Kon het product niet verwijderen"
                else
                    customErrorMessage = "Kon het product niet verwijderen"
                _status.value = KlimaatMobielApiStatus.ERROR

            }catch (e: ConnectException) {
                customErrorMessage = "Er is geen internet! probeer later opnieuw"
                _status.value = KlimaatMobielApiStatus.ERROR

            } catch (e: Exception) {
                customErrorMessage = e.message!!
                _status.value = KlimaatMobielApiStatus.ERROR
            }
        }
    }

    fun onProductClicked(product: Product, action: Int) {
        when(action) {
            0 -> {
                //Animations().toggleArrow(view);
                addProductToOrder(product)
            }
            1 -> {
                PusherApplication.huidigProductId = product.productId
                _navigateToProductDetail.value = listOf(product.projectId, product.productId)
                Timber.i("productid: ${product.projectId} and ${product.productId}")
            }
        }
    }

    fun sortList(adapter: ProductListAdapter, sortStatus: SortStatus) {
        this.sortStatus = sortStatus
        filterList(adapter)
    }

    fun onErrorShown() {
        _status.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    fun clearShoppingCart() {
        viewModelScope.launch {
            val removeAllOrdersDefered = repository.removeAllOrderItems(group.value!!.order.orderId)
            try {
                _status.value = KlimaatMobielApiStatus.LOADING
                val newOrder = removeAllOrdersDefered.await()

                _group.value!!.order.orderItems.removeAll(_group.value!!.order.orderItems)
                _group.value!!.order.submitted = false

                // trigger verandering in winkelmandje
                _group.value = _group.value
                _totaleKlimaatScore.value = 0

                setAantal()
                _deleteClicked.value = false;
                _status.value = KlimaatMobielApiStatus.DONE
            } catch (e: HttpException) {
                if (e.code() == 404)
                    customErrorMessage = "Kon de producten niet verwijderen"
                else
                    customErrorMessage = "Kon de producten niet verwijderen"
                _status.value = KlimaatMobielApiStatus.ERROR

            }catch (e: ConnectException) {
                customErrorMessage = "Er is geen internet! probeer later opnieuw"
                _status.value = KlimaatMobielApiStatus.ERROR

            } catch (e: Exception) {
                customErrorMessage = e.message!!
                _status.value = KlimaatMobielApiStatus.ERROR
            }
        }
    }

    fun onClickDeleteAll() {
        _deleteClicked.value = true;
    }

    fun onDeletedOrCancelled() {
        _deleteClicked.value = false;
    }

    fun confirmOrder(){
        viewModelScope.launch {
            val confirmOrderDeferred = repository.confirmOrder(group.value!!.order.orderId)
            try {
                _status.value = KlimaatMobielApiStatus.LOADING
                val orderRes = confirmOrderDeferred.await()
                _group.value!!.order.submitted = orderRes.submitted

                _group.value = _group.value
                _status.value = KlimaatMobielApiStatus.DONE


            }catch (e: HttpException) {
                if (e.code() == 404)
                    customErrorMessage = "Kon de bestelling niet bevestigen"
                else
                    customErrorMessage = "Kon de bestelling niet bevestigen"
                _status.value = KlimaatMobielApiStatus.ERROR

            }catch (e: ConnectException) {
                customErrorMessage = "Er is geen internet! probeer later opnieuw"
                _status.value = KlimaatMobielApiStatus.ERROR

            } catch (e: Exception) {
                customErrorMessage = e.message!!
                _status.value = KlimaatMobielApiStatus.ERROR
            }
        }
    }
}