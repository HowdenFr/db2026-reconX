// TICKET-ADV123 — React Hook Form + Yup validation.

import React from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { withAuth } from '@components/withAuth.jsx';
import { api } from '@services/apiService.js';

const schema = yup.object({
  tradeRef: yup
    .string()
    .matches(/^[A-Z]{3}-\d{8}-\d{4}$/, 'AAA-YYYYMMDD-NNNN')
    .required('Trade reference is required'),

  instrumentId: yup
    .number()
    .typeError('Instrument ID is required')
    .integer('Instrument ID must be an integer')
    .positive('Instrument ID must be positive')
    .required('Instrument ID is required'),

  counterpartyId: yup
    .number()
    .typeError('Counterparty ID is required')
    .integer('Counterparty ID must be an integer')
    .positive('Counterparty ID must be positive')
    .required('Counterparty ID is required'),

  assetClass: yup
    .string()
    .oneOf(
      ['EQUITY', 'FX', 'BOND', 'DERIVATIVE'],
      'Select a valid asset class'
    )
    .required('Asset class is required'),

  side: yup
    .string()
    .oneOf(
      ['BUY', 'SELL'],
      'Select BUY or SELL'
    )
    .required('Side is required'),

  quantity: yup
    .number()
    .typeError('Quantity is required')
    .positive('Quantity must be greater than zero')
    .required('Quantity is required'),

  price: yup
    .number()
    .typeError('Price is required')
    .positive('Price must be greater than zero')
    .required('Price is required'),

  tradeDate: yup
    .date()
    .required('Trade date is required'),
});

function AddTrade() {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: yupResolver(schema),
    mode: 'onBlur',
    defaultValues: {
      tradeRef: '',
      instrumentId: '',
      counterpartyId: '',
      assetClass: '',
      side: '',
      quantity: '',
      price: '',
      tradeDate: '',
    },
  });

  async function onSubmit(values) {
    try {
      await api.createTrade(values);
      reset();
    } catch (error) {
      console.error(error);
    }
  }

  return (
    <section>
      <h2>Add Trade</h2>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="trade-form"
        noValidate
      >
        <label>
          Trade Reference
          <input
            {...register('tradeRef')}
            placeholder="EQU-20260603-0001"
          />
        </label>
        {errors.tradeRef && (
          <p className="form-error" role="alert">
            {errors.tradeRef.message}
          </p>
        )}

        <label>
          Instrument ID
          <input
            type="number"
            {...register('instrumentId')}
          />
        </label>
        {errors.instrumentId && (
          <p className="form-error" role="alert">
            {errors.instrumentId.message}
          </p>
        )}

        <label>
          Counterparty ID
          <input
            type="number"
            {...register('counterpartyId')}
          />
        </label>
        {errors.counterpartyId && (
          <p className="form-error" role="alert">
            {errors.counterpartyId.message}
          </p>
        )}

        <label>
          Asset Class
          <select {...register('assetClass')}>
            <option value="">Select...</option>
            <option value="EQUITY">EQUITY</option>
            <option value="FX">FX</option>
            <option value="BOND">BOND</option>
            <option value="DERIVATIVE">DERIVATIVE</option>
          </select>
        </label>
        {errors.assetClass && (
          <p className="form-error" role="alert">
            {errors.assetClass.message}
          </p>
        )}

        <label>
          Side
          <select {...register('side')}>
            <option value="">Select...</option>
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
          </select>
        </label>
        {errors.side && (
          <p className="form-error" role="alert">
            {errors.side.message}
          </p>
        )}

        <label>
          Quantity
          <input
            type="number"
            step="0.0001"
            {...register('quantity')}
          />
        </label>
        {errors.quantity && (
          <p className="form-error" role="alert">
            {errors.quantity.message}
          </p>
        )}

        <label>
          Price
          <input
            type="number"
            step="0.0001"
            {...register('price')}
          />
        </label>
        {errors.price && (
          <p className="form-error" role="alert">
            {errors.price.message}
          </p>
        )}

        <label>
          Trade Date
          <input
            type="date"
            {...register('tradeDate')}
          />
        </label>
        {errors.tradeDate && (
          <p className="form-error" role="alert">
            {errors.tradeDate.message}
          </p>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
        >
          Submit
        </button>
      </form>
    </section>
  );
}

export default withAuth(AddTrade);