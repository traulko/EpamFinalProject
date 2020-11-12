package com.traulko.project.service.impl;

import com.traulko.project.dao.CustomTransaction;
import com.traulko.project.dao.ProductDao;
import com.traulko.project.dao.impl.ProductDaoImpl;
import com.traulko.project.entity.CustomImage;
import com.traulko.project.entity.Product;
import com.traulko.project.exception.DaoException;
import com.traulko.project.exception.ServiceException;
import com.traulko.project.exception.TransactionException;
import com.traulko.project.service.ProductService;
import com.traulko.project.validator.ProductValidator;

import java.util.List;
import java.util.Optional;

public class ProductServiceImpl implements ProductService {
    private ProductDao productDao = new ProductDaoImpl();
    private CustomTransaction customTransaction = new CustomTransaction();

    @Override
    public boolean add(String productTitle, String price, String description, String imageName) throws ServiceException {
        boolean result = false;
        try {
            if (ProductValidator.isTitleValid(productTitle) && ProductValidator.isPriceValid(price) &&
                    ProductValidator.isDescriptionValid(description)) {
                Product product = new Product();
                product.setTitle(productTitle);
                product.setDescription(description);
                product.setPrice(Double.parseDouble(price));
                CustomImage image = new CustomImage();
                image.setName(imageName);
                product.setImage(image);
                result = customTransaction.addProductAndImage(product);
            }
        } catch (TransactionException e) {
            throw new ServiceException("Error while adding product", e);
        }
        return result;
    }

    @Override
    public Optional<Product> findById(String id) throws ServiceException {
        Optional<Product> optionalProduct = Optional.empty();
        try {
            if (ProductValidator.isIdValid(id)) {
                int productId = Integer.parseInt(id);
                optionalProduct = productDao.findById(productId);
            }
            return optionalProduct;
        } catch (DaoException e) {
            throw new ServiceException("Error while finding product by id", e);
        }
    }

    @Override
    public List<Product> findBySearchQuery(String searchQuery) throws ServiceException {
        try {
            return productDao.findBySearchQuery(searchQuery);
        } catch (DaoException e) {
            throw new ServiceException("Error while finding products by search query", e);
        }
    }

    @Override
    public List<Product> findAll() throws ServiceException {
        try {
            return productDao.findAll();
        } catch (DaoException e) {
            throw new ServiceException("Error while finding all products", e);
        }
    }
}
